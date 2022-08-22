from turtle import width
import streamlit as st
import pandas as pd
from pinotdb import connect
from datetime import datetime
import time
import plotly.express as px

conn = connect("localhost", 8099)

st.set_page_config(layout="wide")
st.title("Pizza App Dashboard 🍕")

now = datetime.now()
dt_string = now.strftime("%d %B %Y %H:%M:%S")
st.write(f"Last update: {dt_string}")

# Use session state to keep track of whether we need to auto refresh the page and the refresh frequency

if not "sleep_time" in st.session_state:
    st.session_state.sleep_time = 2

if not "auto_refresh" in st.session_state:
    st.session_state.auto_refresh = True

auto_refresh = st.checkbox('Auto Refresh?', st.session_state.auto_refresh)

if auto_refresh:
    number = st.number_input('Refresh rate in seconds', value=st.session_state.sleep_time)
    st.session_state.sleep_time = number


curs = conn.cursor()

query = """
select count(*) FILTER(WHERE  ts > ago('PT1M')) AS events1Min,
       count(*) FILTER(WHERE  ts <= ago('PT1M') AND ts > ago('PT2M')) AS events1Min2Min,
       sum(total) FILTER(WHERE  ts > ago('PT1M')) AS total1Min,
       sum(total) FILTER(WHERE  ts <= ago('PT1M') AND ts > ago('PT2M')) AS total1Min2Min
from orders 
where ts > ago('PT2M')
limit 1
"""
curs.execute(query)

df = pd.DataFrame(curs, columns=[item[0] for item in curs.description])

st.subheader("Orders in the last minute")

metric1, metric2 = st.columns(2)


metric1.metric(
    label="# of Orders",
    value="{:,}".format(int(df['events1Min'].values[0])),
    delta="{:,}".format(int(df['events1Min'].values[0] - df['events1Min2Min'].values[0]))
)


metric2.metric(
    label="Revenue in ₹",
    value="{:,.2f}".format(df['total1Min'].values[0]),
    delta="{:,.2f}".format(df['total1Min'].values[0] - df['total1Min2Min'].values[0])
)

query = """
select ToDateTime(DATETRUNC('minute', ts), 'yyyy-MM-dd hh:mm:ss') AS dateMin, 
      count(*) AS orders, 
    sum(total) AS revenue
from orders 
where ts > ago('PT1H')
group by dateMin
order by dateMin desc
LIMIT 10000
"""

curs.execute(query)

df_ts = pd.DataFrame(curs, columns=[item[0] for item in curs.description])
df_ts_melt = pd.melt(df_ts, id_vars=['dateMin'], value_vars=['revenue', 'orders'])

col1, col2 = st.columns(2)

with col1:
    fig1 = px.line(df_ts_melt[df_ts_melt.variable == "revenue"], x='dateMin', y="value")
    fig1['layout'].update(margin=dict(l=0,r=0,b=0,t=40), title="Revenue per minute")
    fig1.update_yaxes(range=[0, df_ts["revenue"].max() * 1.1])
    st.plotly_chart(fig1, use_container_width=True)

with col2:
    fig2 = px.line(df_ts_melt[df_ts_melt.variable == "orders"], x='dateMin', y="value",color_discrete_sequence =['green'])
    fig2['layout'].update(margin=dict(l=0,r=0,b=0,t=40), title="Orders per minute")
    fig2.update_yaxes(range=[0, df_ts["orders"].max() * 1.1])
    st.plotly_chart(fig2, use_container_width=True)

curs.execute("""
SELECT ts, productId, quantity, status, total, userId
FROM orders
ORDER BY ts DESC
LIMIT 10
""")

df = pd.DataFrame(curs, columns=[item[0] for item in curs.description])

st.subheader("Latest Orders")

# CSS to inject contained in a string
hide_table_row_index = """
            <style>
            thead tr th:first-child {display:none}
            tbody th {display:none}
            </style>
            """

# Inject CSS with Markdown
st.markdown(hide_table_row_index, unsafe_allow_html=True)


st.write(df, width="100%")

if auto_refresh:
    time.sleep(number)
    st.experimental_rerun()