var recording = false;
document.getElementById("speak-bubble").addEventListener("click", () => {
    recording = !recording;
    //AndroidAPI.setRecording(recording);
    addQuestion("Hello???");
    addResponse("Hello!!!");
})

function addQuestion(question){
    const template = document.querySelector("template");
    const clone = template.content.firstElementChild.cloneNode(true);
    const p =  clone.querySelectorAll("p");
    p[0].innerHTML = question;
    const main = document.querySelector("main");
    main.appendChild(clone);
}

function addResponse(response){
    const template = document.querySelector("template");
    const clone = template.content.lastElementChild.cloneNode(true);
    const p =  clone.querySelectorAll("p");
    p[0].innerHTML = response;
    const main = document.querySelector("main");
    main.appendChild(clone);
}
