// ==========================================
// 1. INITIALIZATION (Runs when page loads)
// ==========================================
document.addEventListener("DOMContentLoaded", () => {
    applySettings();
    autoBookmark();   // Silently saves progress if on a chapter
    checkBookmark();  // Shows the resume button if on the TOC
    requestWakeLock(); // Keeps the screen from turning off
});

// ==========================================
// 2. SETTINGS & PERSISTENCE
// ==========================================
function applySettings() {
    // Apply Theme (Fixed to include Sepia)
    const savedTheme = localStorage.getItem("theme");
    if (savedTheme === "light") {
        document.body.classList.add("light-mode");
    } else if (savedTheme === "sepia") {
        document.body.classList.add("sepia-mode");
    }
    
    // Apply Font Family
    if (localStorage.getItem("fontFamily") === "serif") {
        document.body.classList.add("font-switcher");
    }
    
    // Apply Font Size
    const savedSize = localStorage.getItem("fontSize") || "xx-large";
    document.body.style.fontSize = savedSize;

    updateButtons();
}

function updateButtons() {
    const themeBtn = document.getElementById("darkModeButton");
    const fontBtn = document.getElementById("serifButton");
    
    // Consolidated Theme Button Logic
    if (themeBtn) {
        if (document.body.classList.contains("light-mode")) {
            themeBtn.innerText = "🌞";
            themeBtn.className = "button-light";
        } else if (document.body.classList.contains("sepia-mode")) {
            themeBtn.innerText = "📜";
            themeBtn.className = "button-light"; // Or create a button-sepia class in CSS
        } else {
            themeBtn.innerText = "🌑";
            themeBtn.className = "button-dark";
        }
    }
    
    if (fontBtn) {
        fontBtn.innerText = document.body.classList.contains("font-switcher") ? "serif" : "sans-serif";
    }
}

// --- Setting Toggles ---
function changeTheme() { // Linked to your darkModeButton
    const body = document.body;
    let newTheme;

    if (body.classList.contains("light-mode")) {
        body.classList.remove("light-mode");
        body.classList.add("sepia-mode");
        newTheme = "sepia";
    } else if (body.classList.contains("sepia-mode")) {
        body.classList.remove("sepia-mode");
        newTheme = "dark";
    } else {
        body.classList.add("light-mode");
        newTheme = "light";
    }

    localStorage.setItem("theme", newTheme);
    updateButtons();
}

// Ensure your HTML button calls changeTheme() instead of darkMode()
function darkMode() { 
    changeTheme(); 
}

function serif() {
    document.body.classList.toggle("font-switcher");
    const isSerif = document.body.classList.contains("font-switcher");
    localStorage.setItem("fontFamily", isSerif ? "serif" : "sans-serif");
    updateButtons();
}

function setSize(size) {
    document.body.style.fontSize = size;
    localStorage.setItem("fontSize", size);
}

function big() { setSize("xxx-large"); }
function medium() { setSize("xx-large"); }
function small() { setSize("large"); }

// ==========================================
// 3. BOOKMARKS & TOC NAVIGATION
// ==========================================
function getNovelName() {
    const pathParts = window.location.pathname.split('/');
    return pathParts[pathParts.length - 2] || "unknown_novel";
}

function autoBookmark() {
    const currentFile = window.location.pathname.split('/').pop();
    if (currentFile.startsWith("chapter_")) {
        const novelName = getNovelName();
        localStorage.setItem("bookmark_" + novelName, currentFile);
    }
}

function checkBookmark() {
    const currentFile = window.location.pathname.split('/').pop();
    if (currentFile === "toc.html" || currentFile === "index.html") {
        const novelName = getNovelName();
        const savedChapter = localStorage.getItem("bookmark_" + novelName);
        const resumeBtn = document.getElementById("resumeBtn");
        
        if (savedChapter && resumeBtn) {
            resumeBtn.style.display = "block";
            const cleanName = savedChapter.replace(".html", "").replace("chapter_", "Chapter ");
            resumeBtn.innerText = "📖 Resume " + cleanName;
            resumeBtn.onclick = () => { window.location.href = savedChapter; };
        }
    }
}

function jumpToChapter() {
    const chapNum = document.getElementById("jumpInput").value;
    if (chapNum && !isNaN(chapNum)) {
        window.location.href = "chapter_" + chapNum + ".html";
    }
}

// ==========================================
// 4. GESTURES & SCREEN BEHAVIOR
// ==========================================

// --- Screen Wake Lock ---
let wakeLock = null;
async function requestWakeLock() {
    try {
        if ('wakeLock' in navigator) {
            wakeLock = await navigator.wakeLock.request('screen');
        }
    } catch (err) {
        console.log("Wake Lock error:", err.message);
    }
}

document.addEventListener('visibilitychange', () => {
    if (wakeLock !== null && document.visibilityState === 'visible') {
        requestWakeLock();
    }
});

// --- Scroll Progress Bar ---
window.addEventListener('scroll', () => {
    const winScroll = document.body.scrollTop || document.documentElement.scrollTop;
    const height = document.documentElement.scrollHeight - document.documentElement.clientHeight;
    const scrolled = (winScroll / height) * 100;
    const bar = document.getElementById("myBar");
    if (bar) bar.style.width = scrolled + "%";
}, { passive: true });

// --- Swipe Navigation Logic ---
let touchstartX = 0, touchendX = 0, touchstartY = 0, touchendY = 0;

document.addEventListener('touchstart', e => {
    touchstartX = e.changedTouches[0].screenX;
    touchstartY = e.changedTouches[0].screenY;
}, { passive: true });

document.addEventListener('touchend', e => {
    touchendX = e.changedTouches[0].screenX;
    touchendY = e.changedTouches[0].screenY;
    handleGesture();
}, { passive: true });

function handleGesture() {
    const deltaX = touchendX - touchstartX;
    const deltaY = Math.abs(touchendY - touchstartY);
    
    // Ensure the swipe is mostly horizontal and >50px
    if (deltaY < 50 && Math.abs(deltaX) > 50) {
        if (deltaX < 0) { // Swiped left
            const nextLink = document.getElementById("nextLink");
            if (nextLink && nextLink.getAttribute("href") && nextLink.getAttribute("href") !== "没有了") {
                window.location.href = nextLink.href;
            }
        } else if (deltaX > 0) { // Swiped right
            const prevLink = document.getElementById("prevLink");
            if (prevLink && prevLink.getAttribute("href") && prevLink.getAttribute("href") !== "") {
                window.location.href = prevLink.href;
            }
        }
    }
}

// --- One-Handed Tap Navigation (Added Back!) ---
document.addEventListener('click', (e) => {
    const targetTag = e.target.tagName.toUpperCase();
    if (targetTag === 'A' || targetTag === 'BUTTON' || targetTag === 'INPUT') {
        return;
    }

    const screenHeight = window.innerHeight;
    const clickY = e.clientY;

    if (clickY > screenHeight * 0.70) {
        window.scrollBy({ top: screenHeight * 0.85, behavior: 'smooth' });
    } else if (clickY < screenHeight * 0.30) {
        window.scrollBy({ top: -(screenHeight * 0.85), behavior: 'smooth' });
    }
});

// ==========================================
// 1. INITIALIZATION (Runs when page loads)
// ==========================================
document.addEventListener("DOMContentLoaded", () => {
    applySettings();
    autoBookmark();   
    checkBookmark();  
    requestWakeLock(); 
    paginateTOC(); 

    // --- REGISTER SERVICE WORKER FOR PWA ---
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('/sw.js')
            .then(() => console.log("Service Worker Registered!"))
            .catch(err => console.log("Service Worker Failed:", err));
    }
});

// ==========================================
// 5. TOC PAGINATION
// ==========================================
function paginateTOC() {
    const currentFile = window.location.pathname.split('/').pop();
    if (currentFile !== "toc.html") return;

    const tocContainer = document.querySelector('.toc-links');
    if (!tocContainer) return;

    // Grab all the raw links generated by Java
    const links = Array.from(tocContainer.querySelectorAll('a'));
    if (links.length === 0) return;

    // Clear out the container (this deletes all the messy <br> or <p> tags Java originally made!)
    tocContainer.innerHTML = ''; 

    const itemsPerPage = 50;
    const totalPages = Math.ceil(links.length / itemsPerPage);

    if (totalPages > 1) {
        // Create top and bottom pagination rows
        const paginationTop = document.createElement('div');
        paginationTop.className = 'pagination';
        const paginationBottom = document.createElement('div');
        paginationBottom.className = 'pagination';

        for (let i = 1; i <= totalPages; i++) {
            const btn = document.createElement('button');
            btn.innerText = i;
            btn.className = 'page-btn';
            btn.onclick = () => showPage(i, links, itemsPerPage, tocContainer, true);
            paginationTop.appendChild(btn);

            const btnBottom = btn.cloneNode(true);
            btnBottom.onclick = () => showPage(i, links, itemsPerPage, tocContainer, true);
            paginationBottom.appendChild(btnBottom);
        }

        // Insert the rows above and below the TOC list
        tocContainer.parentNode.insertBefore(paginationTop, tocContainer);
        tocContainer.parentNode.insertBefore(paginationBottom, tocContainer.nextSibling);

        // Auto-open to the page containing your bookmark!
        let startPage = 1;
        const novelName = getNovelName();
        const savedChapter = localStorage.getItem("bookmark_" + novelName);
        if (savedChapter) {
            const savedIndex = links.findIndex(a => a.getAttribute('href') === savedChapter);
            if (savedIndex !== -1) {
                startPage = Math.floor(savedIndex / itemsPerPage) + 1;
            }
        }
        showPage(startPage, links, itemsPerPage, tocContainer, false);
    } else {
        // If the book is short (<50 chapters), just show them all beautifully
        showPage(1, links, links.length, tocContainer, false);
    }
}

function showPage(pageNum, links, itemsPerPage, tocContainer, isClick) {
    tocContainer.innerHTML = ''; // clear current links on the screen
    
    const start = (pageNum - 1) * itemsPerPage;
    const end = start + itemsPerPage;
    
    for (let i = start; i < end && i < links.length; i++) {
        links[i].className = 'toc-item'; // Apply the new beautiful CSS class
        tocContainer.appendChild(links[i]);
    }

    // Highlight the active page button
    document.querySelectorAll('.page-btn').forEach(btn => {
        btn.classList.remove('active');
        if (parseInt(btn.innerText) === pageNum) {
            btn.classList.add('active');
        }
    });

    // If the user manually tapped a page button, scroll them back to the top of the TOC list
    if (isClick) {
        tocContainer.scrollIntoView({ behavior: 'smooth' });
    }
}