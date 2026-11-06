(function () {
    "use strict";

    const MORE_TEXT = "Xem thêm";
    const LESS_TEXT = "Thu gọn";

    // ---------- Xem them / Thu gon ----------
    function setupMoreButtons() {
        document.querySelectorAll("[data-filter-more]").forEach((button) => {
            const list = document.getElementById(button.dataset.filterMore);

            if (!list) {
                return;
            }

            const extraItems = list.querySelectorAll(".filter-extra");
            if (extraItems.length === 0) {
                return;
            }
            const hasActiveExtra = list.querySelector(
                ".filter-extra .is-active, .filter-extra input:checked"
            );

            button.hidden = false;

            if (hasActiveExtra) {
                button.textContent = LESS_TEXT;
                button.setAttribute("aria-expanded", "true");
            } else {
                list.classList.add("is-collapsed");
                button.textContent = MORE_TEXT;
                button.setAttribute("aria-expanded", "false");
            }

            button.addEventListener("click", () => {
                const collapsed = list.classList.toggle("is-collapsed");
                button.textContent = collapsed ? MORE_TEXT : LESS_TEXT;
                button.setAttribute("aria-expanded", String(!collapsed));
            });
        });
    }

    // ---------- Thuong hieu: chon la loc ----------
    function setupBrandAutoSubmit() {
        const form = document.getElementById("product-filter-form");

        if (!form) {
            return;
        }

        form.querySelectorAll(".brand-input").forEach((input) => {
            input.addEventListener("change", () => {
                if (form.requestSubmit) {
                    form.requestSubmit();
                } else {
                    form.submit();
                }
            });
        });
    }

    // ---------- Thanh keo gia hai dau ----------
    function setupPriceRange() {
        const form = document.getElementById("product-filter-form");
        const slider = document.getElementById("price-slider");
        const rangeMin = document.getElementById("price-range-min");
        const rangeMax = document.getElementById("price-range-max");
        const textMin = document.getElementById("price-min-input");
        const textMax = document.getElementById("price-max-input");
        const hiddenMin = document.getElementById("price-min-value");
        const hiddenMax = document.getElementById("price-max-value");

        if (!form || !slider || !rangeMin || !rangeMax || !textMin || !textMax || !hiddenMin || !hiddenMax) {
            return;
        }

        const limitMin = Number(slider.dataset.min);
        const limitMax = Number(slider.dataset.max);
        const step = Number(slider.dataset.step) || 1000;
        const formatter = new Intl.NumberFormat("vi-VN");

        if (!Number.isFinite(limitMin) || !Number.isFinite(limitMax) || limitMax <= limitMin) {
            [rangeMin, rangeMax, textMin, textMax].forEach((el) => {
                el.disabled = true;
            });
            return;
        }

        [rangeMin, rangeMax].forEach((el) => {
            el.min = String(limitMin);
            el.max = String(limitMax);
            el.step = String(step);
        });

        function clamp(value, low, high) {
            return Math.min(Math.max(value, low), high);
        }

        function readNumber(text, fallback) {
            const digits = String(text).replace(/\D/g, "");
            return digits === "" ? fallback : Number(digits);
        }

        let low = clamp(readNumber(hiddenMin.value, limitMin), limitMin, limitMax);
        let high = clamp(readNumber(hiddenMax.value, limitMax), limitMin, limitMax);

        if (low > high) {
            low = high;
        }

        function render() {
            rangeMin.value = String(low);
            rangeMax.value = String(high);
            textMin.value = formatter.format(low);
            textMax.value = formatter.format(high);

            const span = limitMax - limitMin;
            const startPercent = ((low - limitMin) / span) * 100;
            const endPercent = 100 - ((high - limitMin) / span) * 100;

            slider.style.setProperty("--range-start", startPercent + "%");
            slider.style.setProperty("--range-end", endPercent + "%");

            const middle = (limitMin + limitMax) / 2;
            rangeMin.classList.toggle("is-on-top", low >= middle);
        }

        rangeMin.addEventListener("input", () => {
            low = Math.min(Number(rangeMin.value), high);
            render();
        });

        rangeMax.addEventListener("input", () => {
            high = Math.max(Number(rangeMax.value), low);
            render();
        });

        [textMin, textMax].forEach((input) => {
            input.addEventListener("input", () => {
                const digits = input.value.replace(/\D/g, "");
                input.value = digits === "" ? "" : formatter.format(Number(digits));
            });
        });

        function commitMin() {
            low = clamp(readNumber(textMin.value, limitMin), limitMin, high);
            render();
        }

        function commitMax() {
            high = clamp(readNumber(textMax.value, limitMax), low, limitMax);
            render();
        }

        textMin.addEventListener("change", commitMin);
        textMax.addEventListener("change", commitMax);

        form.addEventListener("submit", () => {
            if (document.activeElement === textMin) {
                commitMin();
            } else if (document.activeElement === textMax) {
                commitMax();
            }

            hiddenMin.value = low > limitMin ? String(low) : "";
            hiddenMax.value = high < limitMax ? String(high) : "";

            // Gia tri rong thi khong dua len URL
            hiddenMin.disabled = hiddenMin.value === "";
            hiddenMax.disabled = hiddenMax.value === "";
        });

        render();
    }

    function init() {
        setupMoreButtons();
        setupBrandAutoSubmit();
        setupPriceRange();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
