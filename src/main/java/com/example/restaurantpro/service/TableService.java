package com.example.restaurantpro.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.restaurantpro.dto.TableAdminResponseDto;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.BookingStatus;
import com.example.restaurantpro.model.DiningTable;
import com.example.restaurantpro.repository.BookingRepository;
import com.example.restaurantpro.repository.DiningTableRepository;

@Service
public class TableService {

    private final DiningTableRepository diningTableRepository;
    private final BookingRepository bookingRepository;

    public TableService(DiningTableRepository diningTableRepository,
                        BookingRepository bookingRepository) {
        this.diningTableRepository = diningTableRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<DiningTable> getActiveTables() {
        return diningTableRepository.findByActiveTrueOrderByCapacityAsc();
    }

    public List<DiningTable> getAllTables() {
        return diningTableRepository.findAll().stream()
                .sorted((a, b) -> a.getCapacity().compareTo(b.getCapacity()))
                .toList();
    }

    public DiningTable getTableById(Long id) {
        return diningTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bàn."));
    }

    public List<DiningTable> findSuitableTables(Integer guestCount) {
        return diningTableRepository.findByActiveTrueAndCapacityGreaterThanEqualOrderByCapacityAsc(guestCount);
    }

    public List<DiningTable> findExactCapacityTables(Integer guestCount) {
        return diningTableRepository.findByActiveTrueAndCapacityOrderByNameAsc(guestCount);
    }

    public AvailableTablesResult findAvailableExactCapacityTables(Integer guestCount,
                                                                  LocalDateTime bookingDateTime,
                                                                  Integer durationHours) {
        List<DiningTable> exactCapacityTables = findExactCapacityTables(guestCount);
        if (exactCapacityTables.isEmpty()) {
            return new AvailableTablesResult(List.of(), 0, 0);
        }

        int normalizedDuration = (durationHours == null || durationHours < 1) ? 2 : durationHours;
        LocalDateTime fromTime = bookingDateTime;
        LocalDateTime toTime = bookingDateTime.plusHours(normalizedDuration).plusMinutes(30);

        List<DiningTable> availableTables = new ArrayList<>();
        for (DiningTable table : exactCapacityTables) {
            int quantity = normalizeQuantity(table.getQuantity());
            long conflictCount = bookingRepository.countConflictingBookings(table.getId(), fromTime, toTime);
            if (conflictCount < quantity) {
                availableTables.add(table);
            }
        }

        return new AvailableTablesResult(availableTables, exactCapacityTables.size(), availableTables.size());
    }

    public DiningTable save(DiningTable diningTable) {
        return diningTableRepository.save(diningTable);
    }

    public void delete(Long id) {
        if (bookingRepository.existsByDiningTable_Id(id)) {
            throw new IllegalArgumentException("Khong the xoa ban nay khoi CSDL vi da phat sinh booking. Hay xoa booking lien quan truoc.");
        }
        diningTableRepository.deleteById(id);
    }

    public long countTables() {
        return diningTableRepository.count();
    }

    public List<DiningTable> findAll() {
        return getAllTables();
    }

    public DiningTable saveOrUpdate(Long id,
                                    String name,
                                    String floor,
                                    String roomType,
                                    String areaPosition,
                                    Integer capacity,
                                    Integer quantity,
                                    String style,
                                    String chairType,
                                    String description,
                                    boolean active) {
        DiningTable table;
        if (id == null) {
            table = new DiningTable();
        } else {
            table = getTableById(id);
        }
        table.setName(name);
        table.setFloor(floor);
        table.setRoomType(roomType);
        table.setAreaPosition(areaPosition);
        table.setLocation(table.getLocationDisplay());
        table.setCapacity(capacity);
        table.setQuantity(normalizeQuantity(quantity));
        table.setTableType(style);
        table.setChairType(chairType);
        table.setDescription(description);
        table.setActive(active);
        return save(table);
    }

    public List<TableAdminResponseDto> getAdminTableResponses() {
        LocalDateTime now = LocalDateTime.now();
        return getAllTables().stream()
                .filter(DiningTable::isActive)
                .map(table -> {
                    int totalQuantity = normalizeQuantity(table.getQuantity());
                    long activeBookings = bookingRepository.countActiveBookingsAtTime(table.getId(), now);
                    int availableQuantity = (int) Math.max(0, totalQuantity - activeBookings);

                    return new TableAdminResponseDto(
                            table.getId(),
                            table.getName(),
                            table.getTableType(),
                            table.getFloor(),
                            table.getRoomType(),
                            table.getAreaPosition(),
                            table.getLocationDisplay(),
                            table.getCapacity(),
                            totalQuantity,
                            availableQuantity,
                            table.isActive()
                    );
                })
                .toList();
    }

    public TableMonitoringData getTableMonitoringData(LocalDateTime dateTime) {
        LocalDateTime selected = dateTime == null ? LocalDateTime.now() : dateTime;
        List<DiningTable> allTables = getAllTables();
        List<DiningTable> activeTables = allTables.stream().filter(DiningTable::isActive).toList();

        if (activeTables.isEmpty()) {
            return new TableMonitoringData(selected, List.of(), List.of(), 0, 0);
        }

        List<Long> tableIds = activeTables.stream().map(DiningTable::getId).toList();
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                tableIds,
            selected,
            selected.plusMinutes(1),
                List.of(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        Map<Long, Booking> bookingByTableId = overlapping.stream()
                .collect(Collectors.toMap(
                        booking -> booking.getDiningTable().getId(),
                        booking -> booking,
                        (existing, ignored) -> existing
                ));

        List<TableMonitoringRow> monitoringRows = new ArrayList<>();
        for (DiningTable table : activeTables) {
            Booking booking = bookingByTableId.get(table.getId());
            int totalQuantity = normalizeQuantity(table.getQuantity());
            long activeBookingCount = bookingRepository.countActiveBookingsAtTime(table.getId(), selected);
            int occupiedQuantity = (int) Math.min(totalQuantity, activeBookingCount);
            int availableQuantity = Math.max(0, totalQuantity - occupiedQuantity);

            String statusText;
            if (availableQuantity == totalQuantity) {
                statusText = "Còn trống";
            } else if (availableQuantity == 0) {
                statusText = "Hết bàn";
            } else {
                statusText = "Đang sử dụng";
            }

            if (booking == null) {
                monitoringRows.add(new TableMonitoringRow(
                        table,
                        statusText,
                        null,
                        null,
                        totalQuantity,
                        availableQuantity,
                        occupiedQuantity
                ));
            } else {
                monitoringRows.add(new TableMonitoringRow(
                        table,
                        statusText,
                        booking.getBookingDateTime(),
                        booking.getStatus().getDisplayName(),
                        totalQuantity,
                        availableQuantity,
                        occupiedQuantity
                ));
            }
        }

        List<TableZoneSummary> zoneSummaries = monitoringRows.stream()
                .collect(Collectors.groupingBy(row -> new ZoneKey(
                        row.table().getTableType(),
                        row.table().getFloor(),
                        row.table().getRoomType(),
                        row.table().getAreaPosition())))
                .entrySet().stream()
                .map(entry -> {
                    int total = entry.getValue().stream().mapToInt(TableMonitoringRow::totalQuantity).sum();
                    int available = entry.getValue().stream().mapToInt(TableMonitoringRow::availableQuantity).sum();
                    ZoneKey key = entry.getKey();
                String sampleTableName = entry.getValue().stream().map(row -> row.table().getName()).findFirst().orElse("N/A");
                return new TableZoneSummary(sampleTableName, key.tableType(), key.floor(), key.roomType(), key.areaPosition(), total, available);
                })
                .sorted(Comparator.comparing(TableZoneSummary::tableName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        int totalActive = monitoringRows.stream().mapToInt(TableMonitoringRow::totalQuantity).sum();
        int totalAvailable = monitoringRows.stream().mapToInt(TableMonitoringRow::availableQuantity).sum();

        return new TableMonitoringData(selected, monitoringRows, zoneSummaries, totalActive, totalAvailable);
    }

    public record AvailableTablesResult(List<DiningTable> tables, int totalTables, int availableTables) {
    }

    public record TableMonitoringData(LocalDateTime selectedDateTime,
                                      List<TableMonitoringRow> rows,
                                      List<TableZoneSummary> zoneSummaries,
                                      int totalActiveTables,
                                      int totalAvailableTables) {
    }

    public record TableMonitoringRow(DiningTable table,
                                     String statusText,
                                     LocalDateTime bookingDateTime,
                                     String bookingStatus,
                                     int totalQuantity,
                                     int availableQuantity,
                                     int occupiedQuantity) {
    }

    public record TableZoneSummary(String tableName,
                                   String tableType,
                                   String floor,
                                   String roomType,
                                   String areaPosition,
                                   int totalTables,
                                   int availableTables) {
    }

    private record ZoneKey(String tableType, String floor, String roomType, String areaPosition) {
    }

    private int normalizeQuantity(Integer quantity) {
        return quantity != null && quantity > 0 ? quantity : 1;
    }
}