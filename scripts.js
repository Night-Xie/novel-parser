function darkMode() {
    var element = document.body;
    var content = document.getElementById("darkModeButton");
    if(element.classList.toggle("dark-mode"))
    {
        content.innerText = "🌑";
        content.className = "button-dark";
    }
    else
    {
        content.innerText = "🌕";
        content.className = "button-light";
    }
}

function serif() {
    var element = document.body;
    var content = document.getElementById("serifButton");
    if(element.classList.toggle("font-switcher")) {
        content.innerText = "serif";
        content.style.fontFamily = "serif";
    }
    else {
        content.innerText = "sans-serif";
        content.style.fontFamily = "sans-serif";
    }
}

function big() {
    var element = document.body;
    element.style.fontSize = "xxx-large";
}

function medium() {
    var element = document.body;
    element.style.fontSize = "xx-large";
}

function small() {
    var element = document.body;
    element.style.fontSize = "large";
}