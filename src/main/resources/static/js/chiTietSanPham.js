document.addEventListener('DOMContentLoaded', () => {
    const root = document.getElementById('product-detail');
    if (!root) return;

    const photos = [...root.querySelectorAll('.detail-gallery-image')];
    const thumbs = [...root.querySelectorAll('.detail-gallery-thumb')];
    const counter = root.querySelector('#detail-gallery-current');
    const lightbox = root.querySelector('#detail-lightbox');
    const modalImage = root.querySelector('#detail-lightbox-image');
    const modalClose = root.querySelector('#detail-lightbox-close');
    const modalPrev = root.querySelector('#detail-lightbox-prev');
    const modalNext = root.querySelector('#detail-lightbox-next');
    let current = 0;

    function showPhoto(index) {
        if (!photos.length) return;
        current = (index + photos.length) % photos.length;
        photos.forEach((p, i) => p.classList.toggle('is-active', i === current));
        thumbs.forEach((p, i) => {
            p.classList.toggle('is-active', i === current);
            p.setAttribute('aria-pressed', String(i === current));
        });
        if (counter) counter.textContent = String(current + 1);
        if (thumbs[current]) thumbs[current].scrollIntoView({ block: 'nearest', inline: 'nearest' });
        if (lightbox && !lightbox.hidden) setModalImage();
    }
    function setModalImage() {
        if (!modalImage || !photos.length) return;
        const img = photos[current].querySelector('img');
        if (!img) return;
        modalImage.src = img.currentSrc || img.src;
        modalImage.alt = img.alt;
    }
    root.querySelector('#detail-gallery-prev')?.addEventListener('click', () => showPhoto(current - 1));
    root.querySelector('#detail-gallery-next')?.addEventListener('click', () => showPhoto(current + 1));
    thumbs.forEach((thumb, i) => thumb.addEventListener('click', () => showPhoto(i)));

    let lastFocused = null;
    function openModal(trigger) {
        if (!lightbox || !photos.length) return;
        lastFocused = trigger;
        setModalImage();
        lightbox.hidden = false;
        lightbox.setAttribute('aria-hidden', 'false');
        document.body.classList.add('detail-lightbox-open');
        if (modalPrev) modalPrev.hidden = photos.length < 2;
        if (modalNext) modalNext.hidden = photos.length < 2;
        modalClose?.focus();
    }
    function closeModal() {
        if (!lightbox || lightbox.hidden) return;
        lightbox.hidden = true;
        lightbox.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('detail-lightbox-open');
        lastFocused?.focus();
    }
    const zoomButton = root.querySelector('#detail-gallery-zoom');
    zoomButton?.addEventListener('click', () => openModal(zoomButton));
    photos.forEach((photo) => photo.querySelector('img')?.addEventListener('click', () => openModal(zoomButton)));
    modalClose?.addEventListener('click', closeModal);
    modalPrev?.addEventListener('click', () => showPhoto(current - 1));
    modalNext?.addEventListener('click', () => showPhoto(current + 1));
    lightbox?.addEventListener('click', (event) => {
        if (event.target === lightbox) closeModal();
    });
    document.addEventListener('keydown', (event) => {
        if (!lightbox || lightbox.hidden) return;
        if (event.key === 'Escape') closeModal();
        if (event.key === 'ArrowLeft' && photos.length > 1) showPhoto(current - 1);
        if (event.key === 'ArrowRight' && photos.length > 1) showPhoto(current + 1);
        if (event.key === 'Tab' && modalClose) {
            const controls = [modalClose];
            if (photos.length > 1) controls.push(modalPrev, modalNext);
            const index = controls.indexOf(document.activeElement);
            event.preventDefault();
            controls[(index + (event.shiftKey ? controls.length - 1 : 1)) % controls.length].focus();
        }
    });

    // Vuốt ảnh trên điện thoại.
    const stage = root.querySelector('#detail-gallery-stage');
    let touchStart = null;
    if (stage && photos.length > 1) {
        stage.addEventListener('touchstart', e => { touchStart = e.changedTouches[0].screenX; }, { passive: true });
        stage.addEventListener('touchend', e => {
            if (touchStart === null) return;
            const dx = e.changedTouches[0].screenX - touchStart;
            if (Math.abs(dx) > 55) showPhoto(current + (dx < 0 ? 1 : -1));
            touchStart = null;
        }, { passive: true });
    }

    // Mô tả ngắn không có nút Đọc thêm.
    const description = root.querySelector('#detail-description-text');
    const readMore = root.querySelector('#detail-description-more');
    if (description && readMore && description.scrollHeight > 185) {
        description.classList.add('is-collapsed');
        readMore.hidden = false;
        readMore.addEventListener('click', () => {
            const collapsed = description.classList.toggle('is-collapsed');
            readMore.textContent = collapsed ? 'Đọc thêm ▾' : 'Thu gọn ▴';
        });
    }

    // Chỉ lọc danh sách đánh giá đã lấy từ SQL
    const reviewRows = [...root.querySelectorAll('.detail-review-item')];
    const emptyReviews = root.querySelector('#detail-review-filter-empty');
    const ratingFilters = [...root.querySelectorAll('.detail-review-filter button')];
    ratingFilters.forEach(button => button.addEventListener('click', () => {
        const rating = button.dataset.rating;
        ratingFilters.forEach(item => {
            item.classList.toggle('is-active', item === button);
            item.setAttribute('aria-pressed', String(item === button));
        });
        let visible = 0;
        reviewRows.forEach(row => {
            const show = rating === 'all' || row.dataset.rating === rating;
            row.hidden = !show;
            if (show) visible++;
        });
        if (emptyReviews) emptyReviews.hidden = visible !== 0;
    }));

    // Thanh điều hướng tự cập nhật mục đang đọc.
    const quickLinks = [...root.querySelectorAll('.detail-nav-inner a')];
    const sections = quickLinks.map(a => document.querySelector(a.getAttribute('href'))).filter(Boolean);
    if ('IntersectionObserver' in window && sections.length) {
        const observer = new IntersectionObserver(entries => {
            const shown = entries.filter(e => e.isIntersecting)
                .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
            if (shown.length) quickLinks.forEach(link => link.classList.toggle(
                'is-active', link.getAttribute('href') === '#' + shown[0].target.id));
        }, { rootMargin: '-160px 0px -55% 0px', threshold: 0 });
        sections.forEach(section => observer.observe(section));
    }

    // Lưu tối đa 4 sản phẩm đã xem trên cùng trình duyệt.
    async function hienThiSanPhamDaXem() {
        const productId = Number(root.dataset.productId);
        const productName = root.querySelector('#detail-title')?.textContent?.trim();
        if (!Number.isSafeInteger(productId) || productId <= 0 || !productName) return;
        const productBrand = root.dataset.productBrand || '';
        const productPrice = root.querySelector('.detail-price')?.textContent?.trim() || '';
        const rawImage = root.querySelector('.detail-gallery-image.is-active img')?.getAttribute('src') || '';
        const safeImagePath = (value) => {
            if (typeof value !== 'string' || !value) return '';
            try {
                const url = new URL(value, window.location.origin);
                return url.origin === window.location.origin && url.pathname.startsWith('/images/')
                && !url.pathname.includes('..') ? url.pathname : '';
            } catch (_) { return ''; }
        };
        const key = 'pdmjv-recently-viewed-kh07';
        let safeItems = [];
        try {
            const parsed = JSON.parse(window.localStorage.getItem(key) || '[]');
            if (Array.isArray(parsed)) safeItems = parsed.filter(item =>
                item && Number.isSafeInteger(item.id) && item.id > 0
                && typeof item.name === 'string' && typeof item.price === 'string'
                && typeof item.brand === 'string' && typeof item.image === 'string');
        } catch (_) {  }

        const currentProduct = {
            id: productId, name: productName, brand: productBrand,
            price: productPrice, image: safeImagePath(rawImage),

            inStock: root.dataset.productInStock === 'true'
        };
        const recent = [currentProduct, ...safeItems.filter(item => item.id !== productId)].slice(0, 4);

        try {
            const params = new URLSearchParams();
            recent.forEach(item => params.append('maSanPham', String(item.id)));
            const response = await fetch('/san-pham/anh-da-xem?' + params.toString(), {
                headers: { 'Accept': 'application/json' }, credentials: 'same-origin'
            });
            if (response.ok) {
                const currentImages = await response.json();
                recent.forEach(item => {
                    item.image = safeImagePath(currentImages[String(item.id)] || '');
                });
            }
        } catch (_) {  }
        try { window.localStorage.setItem(key, JSON.stringify(recent)); } catch (_) { /* Chế độ riêng tư */ }

        const grid = root.querySelector('#detail-recent-grid');
        const section = root.querySelector('#detail-recently-viewed');
        if (!grid || !section) return;
        grid.replaceChildren();
        recent.forEach(item => {
            const href = `/san-pham/${item.id}`;
            const card = document.createElement('article');
            card.className = 'detail-related-card product-card light';
            const imageLink = document.createElement('a');
            imageLink.className = 'detail-related-photo';
            imageLink.href = href;
            imageLink.setAttribute('aria-label', `Xem ${item.name}`);
            const showPlaceholder = () => {
                imageLink.replaceChildren();
                const placeholder = document.createElement('span');
                const icon = document.createElement('i');
                icon.className = 'bi bi-box-seam';
                icon.setAttribute('aria-hidden', 'true');
                placeholder.append(icon, document.createTextNode('Đang cập nhật ảnh'));
                imageLink.appendChild(placeholder);
            };
            if (item.image) {
                const img = document.createElement('img');
                img.src = item.image;
                img.alt = item.name;
                img.loading = 'lazy';
                img.addEventListener('error', showPlaceholder, { once: true });
                imageLink.appendChild(img);
            } else showPlaceholder();
            const brand = document.createElement('small');
            brand.className = 'detail-related-brand';
            brand.textContent = item.brand;
            const name = document.createElement('a');
            name.className = 'detail-related-name';
            name.href = href;
            name.textContent = item.name;
            const price = document.createElement('strong');
            price.className = 'detail-related-price';
            price.textContent = item.price;

            const actions = document.createElement('div');
            actions.className = 'product-actions';

            const buyButton = document.createElement('button');
            buyButton.type = 'button';
            buyButton.className = 'buy-now-button';
            buyButton.textContent = 'Mua ngay';
            buyButton.disabled = true;
            const cartButton = document.createElement('button');
            cartButton.type = 'button';
            cartButton.className = 'add-to-cart-button';
            cartButton.disabled = true;
            cartButton.title = 'Thêm vào giỏ hàng';
            cartButton.setAttribute('aria-label', `Thêm ${item.name} vào giỏ hàng`);
            const cartIcon = document.createElement('i');
            cartIcon.className = 'bi bi-cart-plus';
            cartIcon.setAttribute('aria-hidden', 'true');
            cartButton.appendChild(cartIcon);
            actions.append(buyButton, cartButton);
            card.append(imageLink, name, brand, price, actions);
            grid.appendChild(card);
        });
        section.hidden = recent.length === 0;
    }
    hienThiSanPhamDaXem().catch(() => {});
    const cartNotice = root.querySelector('#tv2-cart-notice');
    const cartNoticeText = root.querySelector('#tv2-cart-notice-text');
    const cartClose = root.querySelector('#tv2-cart-notice-close');
    let cartNoticeTimeout = null;
    function hideCartNotice() {
        if (cartNotice) cartNotice.hidden = true;
        if (cartNoticeTimeout !== null) clearTimeout(cartNoticeTimeout);
        cartNoticeTimeout = null;
    }
    cartClose?.addEventListener('click', hideCartNotice);
    root.addEventListener('click', (event) => {
        const button = event.target.closest('.tv2-cart-trigger');
        if (!button || !root.contains(button) || button.disabled) return;
        const id = Number(button.dataset.maSanPham);
        if (!Number.isSafeInteger(id) || id <= 0) return;
        const name = button.dataset.tenSanPham || 'sản phẩm';
        // Hook cho TV3: khong xu ly DB, khong tao gio hang rieng.
        const request = new CustomEvent('pdmjv:cart-add-request', {
            bubbles: true, cancelable: true, detail: { maSanPham: id, soLuong: 1 }
        });
        if (!root.dispatchEvent(request)) return; // TV3 da tiep nhan su kien.
        const badge = document.getElementById('cartCount');
        if (badge) {
            const currentCount = Number.parseInt(badge.textContent || '0', 10);
            badge.textContent = String((Number.isFinite(currentCount) ? currentCount : 0) + 1);
        }
        if (cartNotice && cartNoticeText) {
            cartNoticeText.textContent = `Đã chọn “${name}” (demo giao diện, chưa lưu giỏ hàng).`;
            cartNotice.hidden = false;
            if (cartNoticeTimeout !== null) clearTimeout(cartNoticeTimeout);
            cartNoticeTimeout = setTimeout(hideCartNotice, 3500);
        }
    });

    const dock = root.querySelector('#detail-purchase-dock');
    const hero = root.querySelector('.detail-hero');
    const footer = document.querySelector('.footer');
    function updateScroll() {
        if (dock && hero) {
            const pastProduct = hero.getBoundingClientRect().bottom < 90;
            const nearFooter = footer && footer.getBoundingClientRect().top < window.innerHeight;
            dock.hidden = !pastProduct || Boolean(nearFooter);
        }
        document.getElementById('backToTop')?.classList.toggle('show', window.scrollY > 400);
    }
    window.addEventListener('scroll', updateScroll, { passive: true });
    window.addEventListener('resize', updateScroll, { passive: true });
    document.getElementById('backToTop')?.addEventListener('click', () =>
        window.scrollTo({ top: 0, behavior: 'smooth' }));
    updateScroll();
});
