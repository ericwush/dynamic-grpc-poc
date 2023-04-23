package com.example.client;

import com.example.client.Resources.Account;
import com.example.client.Resources.AccountKey;
import com.example.client.Resources.PrimaryKey;
import com.example.client.Resources.Reference;
import com.example.client.Resources.User;
import com.example.client.Resources.UserKey;
import example.ResourceServiceGrpc;
import example.ResourceServiceGrpc.ResourceServiceBlockingStub;
import example.Service.CreateResourceRequest;
import example.Service.CreateResourceResponse;
import example.Service.GetResourceRequest;
import example.Service.GetResourceResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Client {

  private static AccountKey accountKey = AccountKey.newBuilder().setId(1001).build();
  private static UserKey userKey = UserKey.newBuilder().setFirstName("awesome").setLastName("user").build();

  public static void main(String[] args) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
        .usePlaintext()
        .build();

    ResourceServiceBlockingStub stub = ResourceServiceGrpc.newBlockingStub(channel);

    createAccount(stub);
    createUser(stub);
    getUser(stub);

    channel.shutdown();
  }

  private static void createAccount(ResourceServiceBlockingStub stub) {

    Account account = Account.newBuilder().setId(accountKey).setName("Awesome").build();

    CreateResourceRequest request = CreateResourceRequest.newBuilder()
        .setResourceType("Account")
        .setResourceData(account.toByteString())
        .build();

    CreateResourceResponse response = stub.createResource(request);
    System.out.println("Response: " + response);

    // "Success - Account created - Primary Key [1001]"
    // this demonstrates the server can create a specified resource based on the primary key
  }

  private static void createUser(ResourceServiceBlockingStub stub) {
    PrimaryKey accountPK = PrimaryKey.newBuilder().setAccountKey(accountKey).build();
    Reference accountRef = Reference.newBuilder().setRef(accountPK).build();

    UserKey userKey = UserKey.newBuilder().setFirstName("awesome").setLastName("user").build();
    User user = User.newBuilder().setName(userKey).setAccount(accountRef).setEmail("awesome-user@example.com").build();

    CreateResourceRequest request = CreateResourceRequest.newBuilder()
        .setResourceType("User")
        .setResourceData(user.toByteString())
        .build();

    CreateResourceResponse response = stub.createResource(request);
    System.out.println("Response: " + response);

    // "Success - User created - Primary Key [awesome#user], PARENT [account]" <- account's value can also be referenced
    // this demonstrates the server can create a resource based on a composite primary key, as well as the relationships
  }

  private static void getUser(ResourceServiceBlockingStub stub) {
    GetResourceRequest request = GetResourceRequest.newBuilder()
        .setResourceType("User")
        .setPrimaryKey(userKey.toByteString())
        .build();

    GetResourceResponse response = stub.getResource(request);
    System.out.println("Response: " + response);

    // "Success - User fetched - Primary Key [awesome#user]"
    // this demonstrates the server can fetch a resource by its primary key
  }
}
