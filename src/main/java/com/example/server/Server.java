package com.example.server;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ExtensionRegistry;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.io.InputStream;

public class Server {
  public static void main(String[] args) throws IOException, InterruptedException, DescriptorValidationException {
    int port = 50051;
    io.grpc.Server server = ServerBuilder.forPort(port)
        .addService(new ResourceService(loadFileDescriptor()))
        .build()
        .start();

    System.out.println("Server started, listening on " + port);

    server.awaitTermination();
  }

  private static FileDescriptor loadFileDescriptor() throws IOException, DescriptorValidationException {
    FileDescriptorSet descriptorSet;
    FileDescriptor fileDescriptor = null;

    // load the desc file, which can be dynamically registered/injected as well
    String resourceName = "proto/resources.desc";

    try (InputStream inputStream = Server.class.getClassLoader().getResourceAsStream(resourceName)) {
      ExtensionRegistry registry = ExtensionRegistry.newInstance();
      com.example.client.Options.registerAllExtensions(registry);
      DescriptorProtos.registerAllExtensions(registry);
      descriptorSet = FileDescriptorSet.parseFrom(inputStream, registry);

      for (int i = 0; i < descriptorSet.getFileCount(); i++) {
        fileDescriptor = Descriptors.FileDescriptor.buildFrom(descriptorSet.getFile(i), new Descriptors.FileDescriptor[0]);
        for (Descriptors.Descriptor messageDescriptor : fileDescriptor.getMessageTypes()) {
          System.out.println("Message: " + messageDescriptor.getName());
        }
      }
    }

    return fileDescriptor;
  }
}
