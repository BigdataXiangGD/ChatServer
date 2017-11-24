### Server
- ChatServer.java: multiplex IO service, can response to any client socket any time with non-blocking IO.
- ChatService.java: A Class for managing chat rooms. Decoupling handler codes from IO-relative codes.

### How to run programs
1. Use `./compile.sh` to compile source codes.
1. Use `./start.sh <port>` to run chat server.
