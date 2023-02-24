import requests

url = 'http://localhost:5000/message'
data = {'message': 'What languages do you speak?'}

response = requests.post(url, json=data)

print(response.status_code)
print(response.text)
