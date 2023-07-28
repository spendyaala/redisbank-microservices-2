# Deploying to Amazon Elastic Kubernetes Service (EKS)

This version of Redis Bank is excellent for demonstrating the active-active capabilities of Redis Enterprise on managed Kubernetes clusters.

For example, you could deploy the microservices and UI in 'US East' AWS region, and the data generation service in 'US West' - Redis Enterprise will handle the data replication between the regions.

The instructions below detail the steps for getting Redis Enterprise configured on Amazon Elastic Kubernetes Service (EKS) alongside the Redis Bank microservices and UI.

## Prerequisites

1. [An AWS subscription](https://aws.amazon.com/console/)
2. [AWS CLI](https://aws.amazon.com/cli/)
3. [Docker](https://docs.docker.com/get-docker/)
4. A domain name and/or DNS zone that you can configure.
5. [eksctl](https://docs.aws.amazon.com/eks/latest/userguide/eksctl.html)
6. [kubectl](https://kubernetes.io/docs/tasks/tools/)


## Before you begin

If you are not yet a Redis active-active aficionado, you may find it simpler to get started by deploying a standard Redis Enterprise cluster (and databases) on EKS within a single region. Once everything is working end to end, you can deploy a second EKS cluster and configure an active-active database. Finally, deploy the data generation application to a region separate from the UI and microservices (e.g. 'US West').

## AWS CLI configuration
```
aws configure
```
This tool will prompt you to enter the following information, to configure your CLI:
- `AWS Access Key ID [None]:`
- `AWS Secret Access Key [None]`
- `Default region name [None]:`
- `Default output format [None]:`

You can get your `AWS Access Key ID` and `AWS Secret Access Key` from the AWS web console.
Now feed this information to your `aws configure` tool. Here is a typical example:

> NOTE: Do not execute the following snippet. Its just an example of a typical output.
```
[centos@ip-172-31-9-71 lamdba]$ aws configure
AWS Access Key ID [None]: AKIAKERWRWQHTGFSFGF
AWS Secret Access Key [None]: 3+QBPqZEWIFOo5FF0oODFEDgb0Yx4Uk9pcM0MvY
Default region name [None]: us-west-2
Default output format [None]:
```

## Create an ECR repo & authenticate

```
aws ecr create-repository \
--repository-name <REPLACE_THIS_my_private_repo> \
--image-tag-mutability MUTABLE \
--image-scanning-configuration scanOnPush=false \
--region us-east-1
```
You are going to create an ECR repo for each of the micro services you are going to build.

For example: You will create one repo for each microservice.

```
aws ecr create-repository \
--repository-name redisbank-am \
--image-tag-mutability MUTABLE \
--image-scanning-configuration scanOnPush=false \
--region us-east-1

aws ecr create-repository \
--repository-name redisbank-datageneration \
--image-tag-mutability MUTABLE \
--image-scanning-configuration scanOnPush=false \
--region us-east-1

aws ecr create-repository \
--repository-name redisbank-pfm \
--image-tag-mutability MUTABLE \
--image-scanning-configuration scanOnPush=false \
--region us-east-1

aws ecr create-repository \
--repository-name redisbank-transactions \
--image-tag-mutability MUTABLE \
--image-scanning-configuration scanOnPush=false \
--region us-east-1

aws ecr create-repository \
--repository-name redisbank-ui \
--image-tag-mutability MUTABLE \
--image-scanning-configuration scanOnPush=false \
--region us-east-1
```

Now authenticate to the repos.

```
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <REPLACE-THIS_ACCOUNTID>.dkr.ecr.<REGION>.amazonaws.com
```
Example: Here is an example one. Do not run this command as is.

```
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 735486536198.dkr.ecr.us-east-1.amazonaws.com
```

## Build micro services docker images and pushing it to ECR
Now build each micro service docker images and publish it to your repo

Here are typical example commands, for each micro service. Do not use it as is. Replace the Image name and your ECR Repo name.

### Micro Service 1

```
cd redis-bank-am
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=735486936198.dkr.ecr.us-east-1.amazonaws.com/redisbank-am:latest

docker push 735486536198.dkr.ecr.us-east-1.amazonaws.com/redisbank-am:latest
```

### Micro service 2
```
cd redisbank-datageneration
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=735486936198.dkr.ecr.us-east-1.amazonaws.com/redisbank-datageneration:latest

docker push 735486536198.dkr.ecr.us-east-1.amazonaws.com/redisbank-datageneration:latest
```

### Micro service 3
```
cd redisbank-pfm
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=735486936198.dkr.ecr.us-east-1.amazonaws.com/redisbank-pfm:latest

docker push 735486536198.dkr.ecr.us-east-1.amazonaws.com/redisbank-pfm:latest
```

### Micro service 4
```
cd redisbank-transactions
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=735486936198.dkr.ecr.us-east-1.amazonaws.com/redisbank-transactions:latest

docker push 735486536198.dkr.ecr.us-east-1.amazonaws.com/redisbank-transactions:latest
```

### Micro service 5
    ./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=735486936198.dkr.ecr.us-east-1.amazonaws.com/redisbank-ui:latest

    docker push 735486936198.dkr.ecr.us-east-1.amazonaws.com/redisbank-ui:latest

```
cd redisbank-ui
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=735486936198.dkr.ecr.us-east-1.amazonaws.com/redisbank-ui:latest

docker push 735486536198.dkr.ecr.us-east-1.amazonaws.com/redisbank-ui:latest
```

## (OPTIONAL) Deploy Redis Enterprise Cloud in the Cloud (if not using Redis on K8s)
This application can use either a fully managed DBaaS (Database-as-a-Service) from Redis called `Redis Enterprise Cloud` or you can use `Redis Enterprise Software` running off the docker containers.  

If you wan to go with `Redis Enterprise Cloud`, perform these steps.
Otherwise, it is assumed you are going with `Redis Enterprise Software` running off the docker containers. In that case, skip these steps and continue further.

Deploy Redis Enterprise Cloud on AWS at https://app.redislabs.com/#/
You can choose eiether `flexible` or `fixed` subscription.
Deploy a database of size 500MB. Make sure you choose [Redis Stack](https://redis.com/blog/introducing-redis-stack/)

Once you deploy your Redis Enterprise Cloud on AWS, get your database end point details.
For example, it may look something like this:

```
redis_host: redis-15091.c256.us-east-1-2.ec2.cloud.redislabs.com
redis_port: 15091
redis_password: jaS5xwkDhM14Nfjg4V1cmcxSPTyuexbr
```

## Elastic Kubernetes Service

Make sure you installed `eksctl` and `kubectl`.
Ensure your `aws cli` is configured.

Now go ahead and create an EKS cluster.

```
eksctl create cluster --name <REPLACE_THIS_CLUSTER_NAME> --region us-east-1 --nodegroup-name standard-workers --node-type t3.medium --nodes=3 --nodes-min 1 --nodes-max 4 --managed
```

Here is an example:
```
eksctl create cluster --name redis-microserv-eks --region us-east-1 --nodegroup-name standard-workers --node-type t3.medium --nodes=3 --nodes-min 1 --nodes-max 4 --managed
```
This will take 10 to 15 minutes to spin up an EKS cluster.

* For an active-active demo, you need to create two EKS clusters (for example, in separate AWS regions).
* Make sure you meet the minimum required specification (vCPUs and RAM) for Redis Enterprise when configuring your cluster.

## Install Redis Enterprise operator and set up cluster

If you have skipped `Redis Enterprise Cloud` deployment above, then you should perform this step. Otherwise, skip this step and continue, as you already have a fully managed `Redis Enterprise Cloud` running.

Refer to [this documentation](https://docs.redis.com/latest/kubernetes/deployment/quick-start/) to install Redis Enterprise on Kubernetes.

For previous demonstrations, HAProxy has been used for ingress. Be sure to the follow the instructions as outlined in the [Redis ingress controller documentation](https://docs.redis.com/latest/kubernetes/re-databases/set-up-ingress-controller/).

There is a standard custom resource for a cluster in the [../aks/rec](../aks/rec) folder.  Two custom resource definitions for active-active are located in the folder [../aks/rec/active-active](../aks/rec/active-active) to configure RECs in each AKS deployment.

## Configure databases

Redis Bank utilizes four databases (one active-active database which contains a stream, and individual databases for each respective microservice - personal finance management, transactions and account management).

You may choose to expose these databases via ingress controllers, or route traffic internally and leverage the services that make these databases available inside Kubernetes.

Here is an example of using `crdb-cli` to create an active-active database. In this following example the following values are being used in the placeholders described in the [documentation](https://docs.redis.com/latest/kubernetes/active-active/create-aa-database/):

* Kubernetes namespace: ns-uksouth and ns-ukwest
* Cluster names: rec-uksouth and rec-ukwest
* Wildcard DNS records: *.uksouth.aks.demo.redislabs.com and *.ukwest.aks.demo.redislabs.com

```
crdb-cli crdb create \
  --name eventbus \
  --memory-size 200mb \
  --encryption yes \
  --instance fqdn=rec-uksouth.ns-uksouth.svc.cluster.local,url=https://api.uksouth.aks.demo.redislabs.com,username=demo@redislabs.com,password=<password>,replication_endpoint=eventbus-cluster.uksouth.aks.demo.redislabs.com:443,replication_tls_sni=eventbus-cluster.uksouth.aks.demo.redislabs.com \
  --instance fqdn=rec-ukwest.ns-ukwest.svc.cluster.local,url=https://api.ukwest.aks.demo.redislabs.com,username=demo@redislabs.com,password=<password>,replication_endpoint=eventbus-cluster.ukwest.aks.demo.redislabs.com:443,replication_tls_sni=eventbus-cluster.ukwest.aks.demo.redislabs.com

```
> **_NOTE:_**  Once the database has been created, you will need to log in to each cluster and 'enable TLS for all communications'.

> **_NOTE:_** The active-active database command above isn't using the password flag. If testing the deployments using the [standard database example](../aks/db/eventbus-db.yaml), you can set an empty password by creating a Kubernetes secret before you create the database: `kubectl create secret generic redb-eventbus --from-literal=password=''`


## Deployments, services and ingress

Once you have container images in your Azure Container Registry, it's simply a matter of updating the Kubernetes manifests to match your configuration, and deploy them via `kubectl`.

Check the deployment files for the databases in the [../aks/db](../aks/db) folder and the apps in the [../aks/apps](../aks/apps/) folder and replace the relevant placeholders.

For example, to deploy the account management microservice:

```
kubectl apply -f am-app.service.yaml \
-f am-app.ingress.yaml \
-f am-app.deployment.yaml
```

When configuring the manifests, you can reference Redis connection settings using Kubernetes environment variables, or Kubernetes secrets.

Environment variables:

```
env:
- name: SPRING_REDIS_HOST
  value: $(AM_DB_SERVICE_HOST)
- name: SPRING_REDIS_HOST
  value: $(AM_DB_SERVICE_PORT)
```

Kubernetes secrets:
```
env:
- name: SPRING_REDIS_HOST
    valueFrom:
      secretKeyRef:
        name: redb-am-db
        key: service_name
- name: SPRING_REDIS_PORT
    valueFrom:
      secretKeyRef:
        name: redb-am-db
        key: port
- name: SPRING_REDIS_PASSWORD
    valueFrom:
      secretKeyRef:
        name: redb-am-db
        key: password
```

Here are a few example reference commands to create secrets and deploying the microservices.

#### Creating secrets
For redb-am-db:
```
kubectl create secret generic redb-am-db --from-literal=service_name=redis-15091.c256.us-east-1-2.ec2.cloud.redislabs.com --from-literal=port=15091 --from-literal=password=jaS5xwkDhM14Nfjg4V1cmcxSPTyuexbr
```

For redb-pfm-db:
```
kubectl create secret generic redb-pfm-db --from-literal=service_name=redis-15091.c256.us-east-1-2.ec2.cloud.redislabs.com --from-literal=port=15091 --from-literal=password=jaS5xwkDhM14Nfjg4V1cmcxSPTyuexbr
```

For redb-tr-db:
```
kubectl create secret generic redb-tr-db --from-literal=service_name=redis-15091.c256.us-east-1-2.ec2.cloud.redislabs.com --from-literal=port=15091 --from-literal=password=jaS5xwkDhM14Nfjg4V1cmcxSPTyuexbr
```
#### Starting the pods.

```
kubectl apply -f am-app.service.yaml -f am-app.ingress.yaml -f am-app.service.yaml
kubectl apply -f dg-app.yaml
kubectl apply -f pfm-app.service.yaml -f pfm-app.ingress.yaml -f pfm-app.service.yaml
kubectl apply -f tr-app.service.yaml -f tr-app.ingress.yaml -f tr-app.service.yaml
kubectl apply -f ui-app.service.yaml -f ui-app.ingress.yaml -f ui-app.service.yaml
```

When done, if you want to shutdown your cluster, simply run this:
```
eksctl get cluster --region=us-east-1
```
Output would look like this:
```
NAME                REGION                EKSCTL CREATED
redis-microserv-eks        us-east-1        True
```
Now delete the cluster.
```
eksctl delete cluster --region=us-east-1 --name=redis-microserv-eks
```

### Questions, support, issues?
Hit that `New Issue` button, or reach out to the author directly.
