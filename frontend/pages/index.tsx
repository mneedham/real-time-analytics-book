import * as React from 'react';
import { useEffect, useState } from 'react';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Link from '../src/Link';

import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';
import { styled, createTheme, ThemeProvider } from '@mui/material/styles';
import PinotMethodUtils from '../src/pinot/PinotMethodUtils';
import { ResultPane, TableData } from 'Models';
import { Autocomplete, Divider, TextField } from '@mui/material';
import { InputTwoTone } from '@mui/icons-material';

const ButtonAppBar = () => {
  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton
            size="large"
            edge="start"
            color="inherit"
            aria-label="menu"
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            All About That Dough
          </Typography>
          <Button color="inherit">Login</Button>
        </Toolbar>
      </AppBar>
    </Box>
  );
}

const mdTheme = createTheme();

export default function Home() {
  const [resultData, setResultData] = useState<TableData>();
  const [orderIds, setOrderIds] = useState<Array<String>>()

  const [selectedOrderId, setSelectedOrderId] = useState<String>();

  const updateOrder = () => {
    executeQuery(
      `select * 
       FROM orders_enriched 
       WHERE id='${selectedOrderId}' 
       ORDER BY ts DESC
       option(skipUpsert=true)`, 
       setResultData
       )      
  }

  const updateOrdersList = () => {
    executeQuery('select id FROM orders ORDER BY ts DESC LIMIT 50', (input => {
      setOrderIds(
        input.records.map(row => row[0])
      )
    }))
  }


  useEffect(() => {
    updateOrder()
  }, [selectedOrderId]);


  useEffect(() => {
    updateOrdersList()
  }, [])

  const executeQuery = async (query: string,fn: (input: TableData) => void) => {
    const url = "sql"
    const params = JSON.stringify({
      sql: `${query}`,
      trace: false,
    });

    const results = await PinotMethodUtils.getQueryResults(
      params,
      `http://localhost:9000/${url}`,
      false
    );

    fn(results.result)
  }

  return (
    <ThemeProvider theme={mdTheme}>
      <ButtonAppBar />
      <Box
        sx={{
          my: 4,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        <Container>

          <div className="flex justify-between">

          <Autocomplete
            disablePortal
            id="combo-box-demo"
            options={orderIds}
            sx={{ width: 300 }}
            renderInput={(params) => <TextField {...params} label="Select   Order ID" />}
            onChange={(_, value) => setSelectedOrderId(value)}
            className="mb-4"
          />

<Button
            className="ml-0 pl-0"
            onClick={() => {
              updateOrdersList()
            }}>
            Refresh Orders
          </Button>

          </div>


          <Typography variant="h4" component="h1" gutterBottom>
            Order Status for {selectedOrderId}
          </Typography>

          <Button
            className="ml-0 pl-0"
            onClick={() => {
              updateOrder()
            }}>
            Refresh Status
          </Button>


          <div>
            {resultData && resultData.records.map(row => (
              <div className={"px-2 py-5 border-2 border-indigo-200 my-5 rounded-lg flex" + (row[3] === "DELIVERED" ? " bg-green-400" : "") }>
                <div className="w-48">{row[4]}</div>
                <div className="font-semibold">{row[3]}</div>
              </div>
            ))}

            {(!resultData || resultData.records.length == 0) && <div>
              No statuses found for order
            </div>}

          </div>
        </Container>
      </Box>
    </ThemeProvider>
  );
}
