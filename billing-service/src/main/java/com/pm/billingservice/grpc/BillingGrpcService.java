package com.pm.billingservice.grpc;

import billing.BillingResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import billing.BillingServiceGrpc.BillingServiceImplBase;

@Slf4j
@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {
    @Override
    public void createBillingAccount(billing.BillingRequest billingRequest,
         StreamObserver<BillingResponse> responseObserver) {
         log.info("createBillingAccount request received {}", billingRequest.toString());

         BillingResponse response = BillingResponse.newBuilder()
                 .setAccountId("12345")
                 .setStatus("ACTIVE")
                 .build();

         responseObserver.onNext(response);
         responseObserver.onCompleted();
    }
}
