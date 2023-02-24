var recording = false;

document.getElementById("speak-bubble").addEventListener("click", () => {
    recording = !recording;
    AndroidAPI.setRecording(recording);
    addMessage(true, "Hello???");
    addMessage(false, "Hello!!!");
})

function addMessage(isUser, msg){
    var template;
    if(isUser)
        template = document.querySelector("template").content.querySelector(".user-message");
    else 
        template = document.querySelector("template").content.querySelector(".assistant-message");
    const clone = template.cloneNode(true);
    const p =  clone.querySelectorAll("p");
    p[0].innerHTML = msg;
    const main = document.querySelector("main");
    main.appendChild(clone);
}
