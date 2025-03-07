<img src="docs/assets/pulceo-logo-color.png" alt="pulceo-logo" width="25%" height="auto"/>

# pulceo-service-manager

[OpenAPI definition for pulceo-service-manager](https://spboehm.github.io/pulceo-service-manager/)

## General Prerequisites

- Make sure that the following ports are available on the local system:
    - `80/tcp`
    - `443/tcp`
    - `40476/tcp` (for k3d API server)

## Quickstart 

[pulceo-resource-manager#quickstart](https://github.com/spboehm/pulceo-resource-manager?tab=readme-ov-file#quickstart-try-locally)

## Create a free MQTT broker (recommended)

- Create a basic MQTT broker on [HiveMQ](https://console.hivemq.cloud/?utm_source=HiveMQ+Pricing+Page&utm_medium=serverless+signup+CTA+Button&utm_campaign=HiveMQ+Cloud+PaaS&utm_content=serverless)
- Make sure that you select the free plan: Serverless (Free)

## Create your own MQTT broker (optional)

**TODO: Add a guide on how to create a local MQTT broker**
```bash
k3d cluster create pulceo-test --api-port 40476 --port 80:80@loadbalancer
```

Supported version:

- k3d version v5.6.0
- k3s version v1.27.4-k3s1 (default)

## Run with k3d

**[TODO]: Add a step to generate the secrets**
- Apply the following kubernetes manifest to the cluster
```bash
kubectl --kubeconfig=/home/$USER/.kube/config create configmap psm-configmap \
  --from-literal=PRM_HOST=pulceo-resource-manager
```
```bash
kubectl --kubeconfig=/home/$USER/.kube/config create secret generic psm-credentials \
  --from-literal=PNA_MQTT_BROKER_URL=${PNA_MQTT_BROKER_URL} \
  --from-literal=PNA_MQTT_CLIENT_USERNAME=${PNA_MQTT_CLIENT_USERNAME} \
  --from-literal=PNA_MQTT_CLIENT_PASSWORD=${PNA_MQTT_CLIENT_PASSWORD}
```
```bash
kubectl apply -f psm-deployment.yaml
```

- Check if everything is running with: `kubectl get deployment`
```
NAME                     READY   UP-TO-DATE   AVAILABLE   AGE
pulceo-service-manager   1/1     1            1           3m9s
```

- Check the exposed services with: `kubectl get svc`
```
NAME                     TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)    AGE
kubernetes               ClusterIP   10.43.0.1     <none>        443/TCP    59m
pulceo-service-manager   ClusterIP   10.43.44.56   <none>        7979/TCP   3m38s
```

pulceo-service-manager is now running and ready to accept workloads under `http://EXTERNAL-IP`

```bash
curl -I http://localhost:80/psm/health
```
```
HTTP/1.1 200 OK
Content-Length: 2
Content-Type: text/plain;charset=UTF-8
Date: Fri, 08 Mar 2024 01:41:00 GMT
```

## Undeploy

```bash
kubectl delete -f psm-deployment.yaml
```

## Start developing

### Preparations

- Install [Docker](https://www.docker.com/) on your machine by following the official installation guide
- Run a local MQTT broker ([Eclipse Mosquitto](https://mosquitto.org/)) on your system via [Docker Compose](https://docs.docker.com/compose/)
- Run a local redis server ([Redis](https://redis.io/)) via [Docker Compose](https://docs.docker.com/compose/)

```bash
docker-compose -f middleware/docker-compose.yml up -d 
```

- Run a local test cluster with k3d
```bash
k3d cluster create pulceo-test --api-port 40476 --port 80:80@loadbalancer
```
