import * as React from 'react';
import { useEffect, useState } from 'react';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

import Button from '@mui/material/Button';
import { createTheme, ThemeProvider } from '@mui/material/styles';
import { User } from 'Models';

import axios from 'axios';
import Link from 'next/link';
import { ButtonAppBar } from '../src/ButtonAppBar';


const mdTheme = createTheme();

export default function Home() {
  const [users, setUsers] = useState<Array<User>>()

  useEffect(() => {
    getUsers(setUsers)
  }, [])

  const getUsers = async (fn: (users:Array<User>) => void) => {
    const res = await axios(`http://localhost:5000/users`)
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
            Users
          </Typography>

          <div>
            {users && users.map(row => (
              <div className={"px-2 py-5 border-2 border-indigo-200 my-5 rounded-lg flex justify-between" }>
                <div className="w-48 font-bold text-lg">{row.userId}</div>
                <div>
                <Link href={`/users/${row.userId}`} className="ml-0 pl-0">
                  <Button className="ml-0 pl-0">View User</Button>
                </Link>
                </div>

              </div>
            ))}

            {(!users || users.length == 0) && <div>
              No users found
            </div>}

          </div>
        </Container>
      </Box>
    </ThemeProvider>
  );
}
