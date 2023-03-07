# app.py
from flask import Flask, request, jsonify
import threading
import time
from queue import Queue
import revChatGPT.V3 as gpt
import json
import random
import string

class GPT_Thread(threading.Thread):
    def __init__(self):
        super().__init__()
        self._stop_event = threading.Event()
        self.request_event = threading.Event()
        self.lock = threading.Lock()
        self.request_data = ""
        self.bot = None
        self.msg = ""
        self.complete = True
        self.conv_id = self.get_random_string(20)

    def stop(self) -> None:
        self._stop_event.set()

    def _stopped(self) -> bool:
        return self._stop_event.is_set()

    def _startup(self) -> None:
        with open('openai.key', 'r') as file:
            self.config = file.read()
        
        self.bot = gpt.Chatbot(api_key=self.config)

    def _shutdown(self) -> None:

        pass

    def _handle(self) -> None:
        print("handle")
        self.request_event.wait()
        print("handle wait over")
        self.request_event.clear()
        self.complete = False
        for r in self.bot.ask_stream(self.request_data, conversation_id = self.conv_id):
            with self.lock:
                self.msg += r

        self.complete = True
        print("handle ok")
        

    def request(self, request_data):
        with self.lock:
            print("request: " + request_data)
            self.request_data = request_data
            self.request_event.set()
            print("request ok")
        
    def get_response(self):
        with self.lock:
            print("get response")
            return {
                    "message": self.msg,
                    "complete": self.complete
                }
    
    def reset_conversation(self):
        with self.lock:
            self.bot.reset_chat()
            self.conv_id = self.get_random_string(20)
    
    def get_random_string(self, length):
        # choose from all lowercase letter
        letters = string.ascii_lowercase
        return ''.join(random.choice(letters) for i in range(length))

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

@app.post("/reset")
def post_message():
    gthread.reset_conversation()
    return "OK"

@app.get("/response")
def get_response():
    return jsonify({"response": gthread.get_response()})

app.run(debug=True)