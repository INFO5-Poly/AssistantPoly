FROM python:3.8.10
COPY . /app
EXPOSE 5000
WORKDIR /app 
RUN pip install -r requirements.txt
CMD ["python", "server.py"]