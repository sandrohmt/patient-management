package com.pm.stack;

import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

public class LocalStack extends Stack {
    private final Vpc vpc;

    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = createVpc();

        DatabaseInstance authServiceDb =
                createDatabase("AuthServiceDb", "auth-service-db");

        DatabaseInstance patientServiceDb =
                createDatabase("PatientServiceDb", "patient-service-db");

        CfnHealthCheck authDbHealthCheck =
                createDbHealthCheck(authServiceDb, "AuthServiceDbHealthCheck");

        CfnHealthCheck patientDbHealthCheck =
                createDbHealthCheck(patientServiceDb, "PatientServiceDbHealthCheck");

        CfnCluster mskCluster = createMskCluster();
    }

    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementVPC") // this quer dizer a propria classe LocalStack
                .vpcName("PatientManagementVPC")
                .maxAzs(2)
                .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName) {
        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_17_2).build()))
                .vpc(vpc) // conectar o banco de dados com a vpc
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO)) // No LocalStack nao importa mas na aws sim
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("admin_user")) // Vai pegar o username e gerar um secret pra esse username e vai aplicar automaticamente pro banco de daods
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY) // Sempre que destruirmos uma stack tambem vai destruir os dados do banco de dados (nao se faz isso em prod)
                .build();
    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort())) // Converte a porta do db para numero
                        .ipAddress(db.getDbInstanceEndpointAddress())
                        .requestInterval(30) // checa a cada 30 segundos
                        .failureThreshold(3) // tentar 3 vezes, 1 a cada 30 segundos antes de reportar uma falha
                        .build())
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
