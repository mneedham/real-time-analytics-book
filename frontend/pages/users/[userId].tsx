import * as React from 'react';
import { useEffect, useState } from 'react';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

import Button from '@mui/material/Button';
import { createTheme, ThemeProvider } from '@mui/material/styles';
import { Order, OrderSummary, ResultPane, TableData } from 'Models';

import { useRouter } from 'next/router'
import Link from 'next/link'

import axios from 'axios';

import { ButtonAppBar } from '../../src/ButtonAppBar';

const mdTheme = createTheme();

export default function Home() {
  const router = useRouter()
  const { userId } = router.query

  const [orders, setOrders] = useState<Array<Order>>()

  useEffect(() => {
    if(userId !== undefined) {
      getOrders(setOrders)
    }
    
  }, [userId])

  const getOrders = async (fn: (orders:Array<OrderSummary>) => void) => {
    const res = await axios(`http://localhost:8082/users/${userId}/orders`)
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
            Orders for user {userId}
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
              <div className={"px-2 py-5 border-2 border-indigo-200 my-5 rounded-lg flex justify-between"}>
                <div className=" flex justify-between w-3/4">
                  <div className="font-bold w-72">{row.ts}</div>
                  <div className="w-96  font-semibold">{row.id}</div>
                  <div className="w-32">{row.price}₹</div>
                </div>
                <div className="">
                 

                  <Link href={`/orders/${row.id}`} className="ml-0 pl-0">
                  <Button
                    className="ml-0 pl-0"
                    >
                    View Order
                  </Button>
                </Link>

                </div>

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
