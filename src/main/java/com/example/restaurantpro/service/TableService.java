package com.example.restaurantpro.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.restaurantpro.model.DiningTable;
import com.example.restaurantpro.repository.DiningTableRepository;

@Service
public class TableService {

    private final DiningTableRepository diningTableRepository;

    public TableService(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
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

    public DiningTable save(DiningTable diningTable) {
        return diningTableRepository.save(diningTable);
    }

    public void delete(Long id) {
        diningTableRepository.deleteById(id);
    }

    public long countTables() {
        return diningTableRepository.count();
    }

    public List<DiningTable> findAll() {
        return getAllTables();
    }

    public DiningTable saveOrUpdate(Long id, String name, String location, Integer capacity, String style, String chairType, String description) {
        DiningTable table;
        if (id == null) {
            table = new DiningTable();
        } else {
            table = getTableById(id);
        }
        table.setName(name);
        table.setLocation(location);
        table.setCapacity(capacity);
        table.setTableType(style);
        table.setChairType(chairType);
        table.setDescription(description);
        table.setActive(true);
        return save(table);
    }
}