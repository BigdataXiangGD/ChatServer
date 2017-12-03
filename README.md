### Server
- ChatServer.java: multiplex IO service, can response to any client socket any time with non-blocking IO.
- ChatService.java: A Class for managing chat rooms. Decoupling handler codes from IO-relative codes.

### How to run programs
1. git clone https://github.com/BigdataXiangGD/ChatServer
1. cd ChatServer/ChatServer
1. Use `./compile.sh` to compile source codes.
1. Use `./start.sh <port>` to run chat server.

- Notice：
1. The ChatServer can not shut down by the given Chat Server Test, you should use `telnet <Server IP> <Server Port>` and then input `KILL_SERVICE` to force the Server to shutdown, thus the test could be forced to end.
1. Execute `./start_client.sh <Server IP> <Server port>` to run client.
1. The ChatClient hasn't `KILL_SERVICE` command. Users can use `telnet <Server IP> <Server Port>` and then input `KILL_SERVICE` to see the result.
