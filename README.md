## Compile proto files
### Generate the descriptor file
```
cd src/main
protoc --proto_path=proto --proto_path=resources/proto --descriptor_set_out=resources/proto/resources.desc resources/proto/resources.proto
```

### Generate classes to be used by the client
```
cd src/main
protoc --proto_path=proto --proto_path=resources/proto --java_out=java resources/proto/resources.proto
```

## Start server
`Server.main`

## Run client
`Client.main`
