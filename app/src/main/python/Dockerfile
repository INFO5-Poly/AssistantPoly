FROM python:3.8.10
COPY . /app
EXPOSE 5000
WORKDIR /app 
RUN pip install flask
RUN pip install revChatGPT
CMD ["python", "server.py"]