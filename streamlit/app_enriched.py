from turtle import width
import streamlit as st
import pandas as pd
from pinotdb import connect
from datetime import datetime
import time
import plotly.express as px
import plotly.graph_objects as go
import os
from dateutil import parser

def path_to_image_html(path):
    return '<img src="' + path + '" width="60" >'

@st.cache
def convert_df(input_df):
     return input_df.to_html(escape=False, formatters=dict(image=path_to_image_html))

pinot_host=os.environ.get("PINOT_SERVER", "pinot-broker")
pinot_port=os.environ.get("PINOT_PORT", 8099)
conn = connect(pinot_host, pinot_port)

st.set_page_config(layout="wide")
st.title("All About That Dough Dashboard 🍕")

now = datetime.now()
dt_string = now.strftime("%d %B %Y %H:%M:%S")
st.write(f"Last update: {dt_string}")

# Use session state to keep track of whether we need to auto refresh the page and the refresh frequency

if not "sleep_time" in st.session_state:
    st.session_state.sleep_time = 2

if not "auto_refresh" in st.session_state:
    st.session_state.auto_refresh = True

mapping = {
    "1 hour": "PT1H",
    "10 minutes": "PT10M",
    "5 minutes": "PT5M"
}

mapping2 = {
    "1 hour": {"period": "PT60M", "granularity": "minute"},
    "30 minutes": {"period": "PT30M", "granularity": "minute"},
    "10 minutes": {"period": "PT10M", "granularity": "second"},
    "5 minutes": {"period": "PT5M", "granularity": "second"}
}

with st.expander("Configure Dashboard", expanded=True):
    left, right = st.columns(2)

    with left:
        auto_refresh = st.checkbox('Auto Refresh?', st.session_state.auto_refresh)

        if auto_refresh:
            number = st.number_input('Refresh rate in seconds', value=st.session_state.sleep_time)
            st.session_state.sleep_time = number

    with right:
            time_ago = st.radio("Time period to cover", mapping2.keys(), horizontal=True, key="time_ago")

curs = conn.cursor()

pinot_available = False
try:
    curs.execute("select * FROM orders where ts > ago('PT2M')")
    if not curs.description:
        st.warning("Connected to Pinot, but no orders imported",icon="⚠️")
    pinot_available = True
except Exception as e:
    st.warning(f"Unable to connect to Apache Pinot [{pinot_host}:{pinot_port}]",icon="⚠️")

if pinot_available:
    query = """
    select count(*) FILTER(WHERE  ts > ago('PT1M')) AS events1Min,
        count(*) FILTER(WHERE  ts <= ago('PT1M') AND ts > ago('PT2M')) AS events1Min2Min,
        sum("order.total") FILTER(WHERE  ts > ago('PT1M')) AS total1Min,
        sum("order.total") FILTER(WHERE  ts <= ago('PT1M') AND ts > ago('PT2M')) AS total1Min2Min
    from orders_enriched
    where ts > ago(%(timeAgo)s)
    limit 1
    """
    
    curs.execute(query, {"timeAgo": mapping2[time_ago]["period"]})

    df = pd.DataFrame(curs, columns=[item[0] for item in curs.description])

    st.subheader(f"Orders in the last {time_ago}")

    metric1, metric2, metric3 = st.columns(3)


    metric1.metric(
        label="# of Orders",
        value="{:,}".format(int(df['events1Min'].values[0])),
        delta="{:,}".format(int(df['events1Min'].values[0] - df['events1Min2Min'].values[0])) if df['events1Min2Min'].values[0] > 0 else None
    )


    metric2.metric(
        label="Revenue in ₹",
        value="{:,.2f}".format(df['total1Min'].values[0]),
        delta="{:,.2f}".format(df['total1Min'].values[0] - df['total1Min2Min'].values[0]) if df['total1Min2Min'].values[0] > 0 else None
    )

    average_order_value_1min = df['total1Min'].values[0] / int(df['events1Min'].values[0])
    average_order_value_1min_2min = df['total1Min2Min'].values[0] / int(df['events1Min2Min'].values[0]) if int(df['events1Min2Min'].values[0]) > 0 else 0

    metric3.metric(
        label="Average order value in ₹",
        value="{:,.2f}".format(average_order_value_1min),
        delta="{:,.2f}".format(average_order_value_1min - average_order_value_1min_2min) if average_order_value_1min_2min > 0 else None
    )

    query = """
    select ToDateTime(DATETRUNC(%(granularity)s, ts), 'yyyy-MM-dd HH:mm:ss') AS dateMin, 
        count(*) AS orders, 
        sum("order.total") AS revenue
    from orders_enriched
    where ts > ago(%(timeAgo)s)
    group by dateMin
    order by dateMin desc
    LIMIT 10000
    """

    curs.execute(query, {"timeAgo": mapping2[time_ago]["period"], "granularity": mapping2[time_ago]["granularity"]})

    df_ts = pd.DataFrame(curs, columns=[item[0] for item in curs.description])

    if df_ts.shape[0] > 1:
        df_ts_melt = pd.melt(df_ts, id_vars=['dateMin'], value_vars=['revenue', 'orders'])

        col1, col2 = st.columns(2)
        with col1:
            orders = df_ts_melt[df_ts_melt.variable == "orders"]
            latest_date = orders.dateMin.max()
            latest_date_but_one = orders.sort_values(by=["dateMin"], ascending=False).iloc[[1]].dateMin.values[0]
            
            revenue_complete = orders[orders.dateMin < latest_date]
            revenue_incomplete = orders[orders.dateMin >= latest_date_but_one]

            fig = go.FigureWidget(data=[
                go.Scatter(x=revenue_complete.dateMin, y=revenue_complete.value, mode='lines', line={'dash': 'solid', 'color': 'green'}),
                go.Scatter(x=revenue_incomplete.dateMin, y=revenue_incomplete.value, mode='lines', line={'dash': 'dash', 'color': 'green'}),
            ])
            fig.update_layout(showlegend=False, title=f"Orders per {mapping2[time_ago]['granularity']}", margin=dict(l=0, r=0, t=40, b=0),)
            fig.update_yaxes(range=[0, df_ts["orders"].max() * 1.1])
            st.plotly_chart(fig, use_container_width=True) 

        with col2:
            revenue = df_ts_melt[df_ts_melt.variable == "revenue"]
            latest_date = revenue.dateMin.max()
            latest_date_but_one = revenue.sort_values(by=["dateMin"], ascending=False).iloc[[1]].dateMin.values[0]
            
            revenue_complete = revenue[revenue.dateMin < latest_date]
            revenue_incomplete = revenue[revenue.dateMin >= latest_date_but_one]

            fig = go.FigureWidget(data=[
                go.Scatter(x=revenue_complete.dateMin, y=revenue_complete.value, mode='lines', line={'dash': 'solid', 'color': 'blue'}),
                go.Scatter(x=revenue_incomplete.dateMin, y=revenue_incomplete.value, mode='lines', line={'dash': 'dash', 'color': 'blue'}),
            ])
            fig.update_layout(showlegend=False, title=f"Revenue per {mapping2[time_ago]['granularity']}", margin=dict(l=0, r=0, t=40, b=0),)
            fig.update_yaxes(range=[0, df_ts["revenue"].max() * 1.1])
            st.plotly_chart(fig, use_container_width=True) 

    # CSS to inject contained in a string
    hide_table_row_index = """
    <style>
    thead tr th:first-child {display:none}
    tbody th {display:none}
    tbody tr th:first-child {display:none}
    div.stMarkdown table.dataframe { 
        width: 100%;
        text-align: center;
        margin-bottom: 2rem;
    }
    div.stMarkdown table.dataframe thead tr {
        text-align: center !important; 
    }

    div.stMarkdown table.dataframe, div.stMarkdown table.dataframe tbody tr td, div.stMarkdown table.dataframe thead tr th {
        border:none
    }

    div.stMarkdown table.dataframe tbody tr, div.stMarkdown table.dataframe thead tr {
        height: 75px;
    }

    div.stMarkdown table.dataframe tbody tr:nth-child(even) {
        background: #efefef
    }             
    </style>
    """

    # Inject CSS with Markdown
    st.markdown(hide_table_row_index, unsafe_allow_html=True)

    left, right = st.columns(2)

    with left:
        st.subheader("Most popular items")

        curs.execute("""
        SELECT "product.name" AS product, 
            "product.image" AS image,
                count(*) AS orders, 
                sum("order.quantity") AS quantity
        FROM orders_enriched
        where ts > ago(%(timeAgo)s)
        group by product, image
        ORDER BY count(*) DESC
        LIMIT 5
        """, {"timeAgo": mapping2[time_ago]["period"]})

        df = pd.DataFrame(curs, columns=[item[0] for item in curs.description])
        df["quantityPerOrder"] = df["quantity"] / df["orders"]

        html = convert_df(df)
        st.markdown(html, unsafe_allow_html=True)

    with right:
        st.subheader("Most popular categories")

        curs.execute("""
        SELECT "product.category" AS category, 
                count(*) AS orders, 
                sum("order.quantity") AS quantity
        FROM orders_enriched
        where ts > ago(%(timeAgo)s)
        group by category
        ORDER BY count(*) DESC
        LIMIT 5
        """, {"timeAgo": mapping2[time_ago]["period"]})

        df = pd.DataFrame(curs, columns=[item[0] for item in curs.description])
        df["quantityPerOrder"] = df["quantity"] / df["orders"]

        html = convert_df(df)
        st.markdown(html, unsafe_allow_html=True)

    curs.execute("""
    SELECT ToDateTime(ts, 'HH:mm:ss:SSS') AS tsms, 
           "product.name" AS product, 
            "product.image" AS image,
            "product.category" AS category,
           "order.quantity" as quantity, 
           "order.status" As status, 
           "order.total" AS total, 
           "order.userId" AS userId
    FROM orders_enriched
    ORDER BY ts DESC
    LIMIT 5
    """)

    df = pd.DataFrame(curs, columns=[item[0] for item in curs.description])
    
    st.subheader("Latest Orders")

    html = convert_df(df)

    st.markdown(
        html,
        unsafe_allow_html=True
    )

    curs.close()

if auto_refresh:
    time.sleep(number)
    st.experimental_rerun()