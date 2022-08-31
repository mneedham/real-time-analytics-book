FROM python:3.9.9-bullseye

WORKDIR /workdir

COPY requirements.txt .

ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

RUN pip install --no-cache-dir -r requirements.txt
RUN pip install --upgrade pip

COPY app.py .

ENTRYPOINT ["streamlit", "run", "app.py"]