package com.pm.billingservice.grpc;

import billing.BillingResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import net.devh.boot.grpc.server.service.GrpcService;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    @Override
    public void createBillingAccount(billing.BillingRequest billingRequest,
         StreamObserver<BillingResponse> responseObserver) {
         log.info("createBillingAccount request received {}", billingRequest.toString());
        // Lógica de negócios relacionados a billing aqui. Ex: Salvar no banco de dados, fazer cálculos, etc.

         BillingResponse response = BillingResponse.newBuilder()
                 .setAccountId("12345")
                 .setStatus("ACTIVE")
                 .build();

         responseObserver.onNext(response);
         responseObserver.onCompleted();
    }
}
