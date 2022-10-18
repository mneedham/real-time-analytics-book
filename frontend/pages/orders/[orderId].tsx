import * as React from 'react';
import { useEffect, useState } from 'react';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';
import { createTheme, ThemeProvider } from '@mui/material/styles';
import { Order, OrderStatus, OrderSummary, ResultPane, TableData } from 'Models';

import { useRouter } from 'next/router'
import Link from 'next/link'

import axios from 'axios';


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
  const router = useRouter()
  const { orderId } = router.query

  const [orders, setOrders] = useState<Array<OrderStatus>>()

  useEffect(() => {
    if(orderId !== undefined) {
      getOrders(setOrders)
    }
    
  }, [orderId])

  const getOrders = async (fn: (orders:Array<OrderSummary>) => void) => {
    const res = await axios(`http://localhost:5000/orders/${orderId}`)
    fn(res.data)
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

      

          <Typography variant="h4" component="h1" gutterBottom>
            Statuses for order {orderId}
          </Typography>

          <Button
            className="ml-0 pl-0"
            onClick={() => {
              getOrders(setOrders)
            }}>
            Refresh Orders
          </Button>


          <div>
            {orders && orders.map(row => (
                 <div className={"px-2 py-5 border-2 border-indigo-200 my-5 rounded-lg flex" + (row.status === "DELIVERED" ? " bg-green-400" : "") }>
                 <div className="w-48">{row.timestamp}</div>
                 <div className="font-semibold">{row.status}</div>
               </div>
            ))}

            {(!orders || orders.length == 0) && <div>
              No orders found for user
            </div>}

          </div>
        </Container>
      </Box>
    </ThemeProvider>
  );
}
