document.addEventListener("DOMContentLoaded", function () {
    const qtyInputs = document.querySelectorAll(".qty-input");
    if (!qtyInputs.length) {
        return;
    }

    const currency = new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND"
    });

    const totalNode = document.getElementById("grandTotal");
    const countNode = document.getElementById("selectedCount");

    function updateSummary() {
        let total = 0;
        let count = 0;

        qtyInputs.forEach((input) => {
            const qty = parseInt(input.value || "0", 10);
            const price = parseFloat(input.dataset.price || "0");
            if (!Number.isNaN(qty) && qty > 0) {
                count += qty;
                total += qty * price;
            }
        });

        if (totalNode) {
            totalNode.textContent = currency.format(total);
        }
        if (countNode) {
            countNode.textContent = `${count} món`;
        }
    }

    qtyInputs.forEach((input) => input.addEventListener("input", updateSummary));
    updateSummary();
});


