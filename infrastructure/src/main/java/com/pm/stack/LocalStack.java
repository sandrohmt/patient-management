package com.pm.stack;

import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalStack extends Stack {
    private final Vpc vpc;
    private final Cluster ecsCluster;

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

        this.ecsCluster = createEcsCluster();

        FargateService authService =
                createFargateService("AuthService",
                        "auth-service",
                        List.of(4005),
                        authServiceDb,
                        Map.of("JWT_SECRET", "scobvP/g3rPtv5bJR0iO/B6jU6T7+PmJSNfNy0fwCASxF0qZyCLjxbUXwP7ie/To"));
        authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDb); // Inicia o db antes de iniciar o auth service

        FargateService billingService =
                createFargateService("BillingService",
                        "billing-service",
                        List.of(4001, 9001), // 9001 eh o port do grpc
                        null,
                        null);

        FargateService analyticsService =
                createFargateService("AnalyticsService",
                        "analytics-service",
                        List.of(4002), // ports do kafka ja foram configurados
                        null,
                        null);
        analyticsService.getNode().addDependency(mskCluster); // analytics service tem uma dependencia com o mskcluster que eh o kafka

        FargateService patientService =
                createFargateService("PatientService",
                        "patient-service",
                        List.of(4000),
                        patientServiceDb,
                        Map.of("BILLING_SERVICE_ADDRESS", "host.docker.internal",
                                "BILLING_SERVICE_GRPC_PORT", "9001"
                                ));
        patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(patientServiceDb); // Inicia o db antes de iniciar o auth service
        patientService.getNode().addDependency(billingService); // Tem comunicacao sincrona com o billing-service
        patientService.getNode().addDependency(
                mskCluster);
        // Essas dependencias sao quais microsservicos precisam estar rodando antes desse rodas, ou seja, suas dependencias

        createApiGatewayService();
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

    private CfnCluster createMskCluster() {
    return CfnCluster.Builder.create(this, "MskCluster")
            .clusterName("kafka-cluster")
            .kafkaVersion("2.8.0")
            .numberOfBrokerNodes(1) // Em prod voce teria mais para alta disponibilidades
            .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                    .instanceType("kafka.m5.xlarge")
                    .clientSubnets(vpc.getPrivateSubnets().stream().map(
                            ISubnet::getSubnetId).collect(Collectors.toList())) // Pega todos os private subnets da vpc como uma lista e passa essa para o clientSubenets como argumento
                    .brokerAzDistribution("DEFAULT").build())
            .build(); // Especifica como os brokers sao distribuidos pelas AZs

    }

    private Cluster createEcsCluster() {
        return software.amazon.awscdk.services.ecs.Cluster.Builder.create(this, "PatientManagementCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("patient-management.local")
                        .build())
                .build();
    }

    private FargateService createFargateService(String id,
        String imageName,
        List<Integer> ports,
        DatabaseInstance db,
        Map<String, String> additionalEnvVars) {

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, id + "Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions.Builder containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry(imageName))

                        .portMappings(ports.stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())

                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                                .logGroupName("/ecs/" + imageName)
                                                .removalPolicy(RemovalPolicy.DESTROY) // Quando a stack for destruida os logs tambem serao
                                                .retention(RetentionDays.ONE_DAY)
                                                .build())
                                        .streamPrefix(imageName) // Mantem os logs organizados pelo nome da imagem
                                .build()));

        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512"); // Esses sao os 3 enderecos que o localstack pode configurar o kafka para ser hosteado

        if (additionalEnvVars != null) {
            envVars.putAll(additionalEnvVars);
        }

        if (db != null) { // Configuraçao caso o microsservico tenha um banco de dados
            envVars.put("SPRING_DATASOURE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imageName));

            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            envVars.put("SPRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "600000"); // Seta um timeout para o caso de um db nao estar pronto, ele tentar mais algumas vezes
        }

        containerOptions.environment(envVars);

        taskDefinition.addContainer(imageName + "Container", containerOptions.build()); // Linkar um container com uma task definition e uma image com um container

        return FargateService.Builder.create(this, id) // Criacao de um fargate service que liga o service com a task definition, que tem um container baseado na imageName encontrada no containerOptions
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false) // Nao vamos colocar os servicos abertos para internet
                .serviceName(imageName)
                .build();
    }

    private void createApiGatewayService() {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();


        ContainerDefinitionOptions containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry("api-gateway"))

                        .environment(Map.of(
                                "SPRING_PROFILES_ACTIVE", "prod",
                                "AUTH_SERVICE_URL", "http://host.docker.internal:4005"
                        ))

                        .portMappings(List.of(4004).stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())

                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup" + "LogGroup")
                                        .logGroupName("/ecs/api-gateway")
                                        .removalPolicy(RemovalPolicy.DESTROY) // Quando a stack for destruida os logs tambem serao
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix("api-gateway") // Mantem os logs organizados pelo nome da imagem
                                .build()))
                        .build();

        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        ApplicationLoadBalancedFargateService apiGateway // o ApplicationLoadBalancedFargateService cria um ALB automaticamente, diferente do FargateService
                = ApplicationLoadBalancedFargateService.Builder.create(this, "ApiGatewayservice")
                .cluster(ecsCluster)
                .serviceName("api-gateway")
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .healthCheckGracePeriod(Duration.seconds(60)) // Quanto tempo o ALB vai esperar o apigateway container começar antes de lançar um erro
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
