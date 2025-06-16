package com.pm.stack;

import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.*;

public class LocalStack extends Stack {
    private final Vpc vpc;

    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = createVpc();
    }

    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementVPC") // this quer dizer a propria classe LocalStack
                .vpcName("PatientManagementVPC")
                .maxAzs(2)
                .build();
    }

    public static void main(String[] args) {
        App app = new App(AppProps.builder().outdir("./cdk.out").build()); // Cria numa pasta cdk dentro de outra chamada out

        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer()) // Converte o codigo num modelo do CloudFormation
                .build();

        new LocalStack(app, "localstack", props);
        app.synth();
        System.out.println("App synthesizing in progress...");
    }
}
