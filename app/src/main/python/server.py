# app.py
from flask import Flask, request, jsonify
import threading
import time
from queue import Queue
import revChatGPT.V1 as gpt



class GPT_Thread(threading.Thread):
    def __init__(self):
        super().__init__()
        self._stop_event = threading.Event()
        self.request_event = threading.Event()
        self.request_processed_event = threading.Event()
        self.request_processed_event.set()
        self.request_data = ""
        self.bot = None
        self.msg = ""
        self.count = 0

    def stop(self) -> None:
        self._stop_event.set()

    def _stopped(self) -> bool:
        return self._stop_event.is_set()

    def _startup(self) -> None:
        with open('config.json', 'r') as file:
            self.config = file.read()
        
        self.bot = gpt.Chatbot(self.config)

    def _shutdown(self) -> None:

        pass

    def _handle(self) -> None:
        print("handle")
        self.request_event.wait()
        print("handle wait over")
        self.request_event.clear()
        for r in self.bot.ask(self.request_data):
            if r == None:
                break;
            self.msg = r["message"]
            self.count += 1
            print(self.count, flush=True)
            
        self.request_processed_event.set()
        print("handle ok")
        

    def request(self, request_data):
        print("request: " + request_data)
        self.request_processed_event.wait()
        self.request_processed_event.clear()
        self.request_data = request_data
        self.request_event.set()
        print("request ok")
    
    def get_response(self):
        print("get response")
        return str(self.count) + " : " + self.msg

    def run(self) -> None:
        self._startup()
        while not self._stopped():
            self._handle()
        self._shutdown()


gthread = GPT_Thread()
gthread.start()

app = Flask(__name__)

@app.post("/message")
def post_message():
    if not request.is_json:
        return {"error": "Request must be JSON"}, 415
    
    r = request.get_json()
    gthread.request(r["message"])
    return "OK"

@app.get("/response")
def get_response():
    return jsonify({"response": gthread.get_response()})

app.run(debug=True)