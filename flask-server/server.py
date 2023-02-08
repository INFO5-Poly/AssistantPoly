from flask import Flask, request, redirect, url_for, flash, jsonify

app = Flask(__name__)

@app.route('/predict', methods=['POST'])

def make_infer():
    """
    Function to calculte the IMC of a person

    Args: 
        - height: height of the person in cm
        - weight: weight of the person in kg
        - name: name of the person
    Returns:
        - imc: IMC of the person
    """

    # Getting the inputs
    data = request.get_json()
    inputs = data["inputs"]

    # IMC calculation
    height = inputs["height"]
    weight = inputs["weight"]
    name = inputs["name"]
    imc = weight / (height/100)**2

    # Serving the output
    outputs = [{"outputs": {"imc": imc, "name": name}}]

    return outputs

@app.route('/', methods=['GET'])

def print_hello():
    return "IMC Calculator"

if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)

