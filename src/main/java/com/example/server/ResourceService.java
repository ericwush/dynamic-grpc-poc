package com.example.server;

import com.example.client.Options;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import example.ResourceServiceGrpc;
import example.Service;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceService extends ResourceServiceGrpc.ResourceServiceImplBase {

  private FileDescriptor fileDescriptor;

  public ResourceService(FileDescriptor fileDescriptor) {
    this.fileDescriptor = fileDescriptor;
  }

  @Override
  public void createResource(Service.CreateResourceRequest request, StreamObserver<Service.CreateResourceResponse> responseObserver) {
    // resource type has to match the message type
    Descriptor resourceDescriptor = fileDescriptor.findMessageTypeByName(request.getResourceType());
    byte[] resourceData = request.getResourceData().toByteArray();
    DynamicMessage resource = null;
    try {
      resource = DynamicMessage.parseFrom(resourceDescriptor, resourceData);
      System.out.println(resource);
    } catch (InvalidProtocolBufferException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid resource data").asRuntimeException());
    }

    StringBuilder status = new StringBuilder();
    status.append("Success - ").append(request.getResourceType()).append(" created - ");
    // extract primary key
    getPrimaryKey(resource).ifPresent(pk -> status.append("Primary Key [").append(pk).append("]"));
    // extract relationship(s)
    getRelationship(resource).forEach((key, value) -> status.append(", ").append(key).append(" [").append(value).append("]"));

    Service.CreateResourceResponse response = Service.CreateResourceResponse.newBuilder()
        .setStatus(status.toString())
        .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getResource(Service.GetResourceRequest request, StreamObserver<Service.GetResourceResponse> responseObserver) {
    Optional<String> primaryKey = getResourceByPrimaryKey(request.getResourceType(), request.getPrimaryKey().toByteArray());

    StringBuilder status = new StringBuilder();
    status.append("Success - ").append(request.getResourceType()).append(" fetched - ");
    primaryKey.ifPresent(pk -> status.append("Primary Key [").append(pk).append("]"));

    Service.GetResourceResponse response = Service.GetResourceResponse.newBuilder()
        .setStatus(status.toString())
        .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private Optional<String> getPrimaryKey(DynamicMessage resource) {
    return resource.getAllFields().entrySet().stream()
        .filter(e -> e.getKey().getJavaType() == FieldDescriptor.JavaType.MESSAGE && isPrimaryKey(e.getKey().getMessageType()))
        .findFirst()
        .map(e -> (DynamicMessage) e.getValue())
        .map(this::concatenatePrimaryKey);
  }

  private String concatenatePrimaryKey(DynamicMessage pk) {
    StringBuilder concatenatedPrimaryKey = new StringBuilder();
    for (FieldDescriptor fieldDescriptor : pk.getAllFields().keySet()) {
      if (concatenatedPrimaryKey.length() > 0) {
        concatenatedPrimaryKey.append("#");
      }
      concatenatedPrimaryKey.append(pk.getField(fieldDescriptor).toString());
    }
    return concatenatedPrimaryKey.toString();
  }

  private boolean isPrimaryKey(Descriptor messageDescriptor) {
    return (Boolean) messageDescriptor.getOptions().getField(Options.primaryKey.getDescriptor());
  }

  private Map<Options.Relationship, String> getRelationship(DynamicMessage resource) {
    return resource.getAllFields().entrySet().stream()
        .filter(e -> e.getKey().getOptions().hasExtension(Options.relationship))
        .collect(Collectors.toMap(
            e -> e.getKey().getOptions().getExtension(Options.relationship),
            e -> e.getKey().getName() // just return the field name, already have access to the field value
        ));
  }

  private Optional<String> getResourceByPrimaryKey(String resourceType, byte[] primaryKey) {
    // just exact the primary key and assume the resource can be fetched
    return fileDescriptor.findMessageTypeByName(resourceType).getFields().stream()
        .filter(field -> field.getJavaType() == FieldDescriptor.JavaType.MESSAGE && isPrimaryKey(field.getMessageType()))
        .findFirst()
        .map(FieldDescriptor::getMessageType)
        .map(desc -> {
          try {
            return DynamicMessage.parseFrom(desc, primaryKey);
          } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
          }
        })
        .map(this::concatenatePrimaryKey);
  }
}
