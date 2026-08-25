# Integración de Kong Gateway con AKS

## 1. Objetivo

Configurar y probar **Kong Gateway** como API Gateway delante de una aplicación Quarkus desplegada en **Azure Kubernetes Service (AKS)**.

El objetivo inicial del laboratorio es validar:

- Instalación de Kong Gateway en AKS.
- Funcionamiento de Kong Ingress Controller (KIC).
- Exposición de Kong mediante un `LoadBalancer`.
- Integración entre Kubernetes `Ingress` y Kong.
- Routing de peticiones hacia un microservicio.
- Separación entre los namespaces de Kong y las aplicaciones.
- Preparación de la arquitectura para incorporar posteriormente autenticación, autorización, rate limiting y otras políticas de seguridad.

---

# 2. Arquitectura

La arquitectura implementada es:

```text
                         Internet
                            |
                            |
                    Azure LoadBalancer
                            |
                    172.168.4.116
                            |
                            v
                 +---------------------+
                 |    Kong Gateway      |
                 |                     |
                 |  HTTP :80            |
                 |  HTTPS :443          |
                 +----------+----------+
                            |
                            | Kong Ingress Controller
                            |
                            v
                 +---------------------+
                 | Kubernetes Ingress  |
                 |                     |
                 | /assistant           |
                 +----------+----------+
                            |
                            v
                 +---------------------+
                 | app-searchia Service|
                 | ClusterIP :80       |
                 +----------+----------+
                            |
                            | 80 -> 8080
                            v
                 +---------------------+
                 | app-searchia Pod    |
                 | Quarkus :8080       |
                 +----------+----------+
                            |
                   +--------+--------+
                   |                 |
                   v                 v
             Azure AI Search   Azure OpenAI
```

---

# 3. Namespaces

Se utilizaron dos namespaces diferentes.

## Namespace `kong`

Contiene los componentes relacionados con Kong:

```bash
kubectl get svc -n kong
```

Resultado relevante:

```text
NAME                       TYPE           EXTERNAL-IP     PORT(S)
kong-gateway-proxy         LoadBalancer   172.168.4.116   80:31584/TCP,443:31713/TCP
kong-gateway-manager       NodePort       <none>          8002:31916/TCP,8445:32594/TCP
kong-gateway-admin         ClusterIP      <none>          8444/TCP
```

El servicio importante para el tráfico de clientes es:

```text
kong-gateway-proxy
```

Su función es recibir las peticiones externas y enviarlas hacia los servicios de Kubernetes según las reglas configuradas.

---

## Namespace `applications`

Contiene la aplicación:

```text
app-searchia
```

El Service es:

```bash
kubectl get svc -n applications
```

Resultado:

```text
NAME           TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)
app-searchia   ClusterIP   10.0.131.39    <none>        80/TCP
```

El Service utiliza:

```text
Port:       80
Target:     8080
```

Por tanto:

```text
app-searchia:80
       |
       v
Pod:8080
```

---

# 4. Verificación del Service

Antes de integrar Kong, se verificó que Kubernetes pudiera llegar correctamente al Pod.

Se ejecutó:

```bash
kubectl get endpoints app-searchia -n applications
```

Resultado:

```text
NAME           ENDPOINTS
app-searchia   10.244.0.164:8080
```

Esto permitió comprobar que el Service:

```text
app-searchia:80
```

tiene un endpoint disponible:

```text
10.244.0.164:8080
```

La advertencia:

```text
v1 Endpoints is deprecated in v1.33+
```

no representa un problema funcional. Kubernetes recomienda actualmente utilizar `EndpointSlice`.

Puede comprobarse mediante:

```bash
kubectl get endpointslice \
  -n applications \
  -l kubernetes.io/service-name=app-searchia
```

---

# 5. Verificación de la aplicación Quarkus

La aplicación `app-searchia` utiliza:

```text
Java 17
Quarkus
Puerto 8080
```

Se comprobó la disponibilidad del endpoint OpenAPI:

```bash
kubectl run curl-test \
  --rm -it \
  --restart=Never \
  --image=curlimages/curl \
  -n applications \
  -- curl -i http://app-searchia/q/openapi
```

La aplicación respondió:

```text
HTTP/1.1 200 OK
Content-Type: application/yaml;charset=UTF-8
```

El contrato OpenAPI permitió identificar el endpoint principal:

```text
POST /assistant/search
```

El request esperado es:

```json
{
  "question": "..."
}
```

Por tanto, la aplicación expone una API HTTP real que puede ser publicada mediante Kong.

---

# 6. Kong Ingress Controller

El componente **Kong Ingress Controller (KIC)** actúa como puente entre los recursos Kubernetes y Kong Gateway.

El flujo conceptual es:

```text
Kubernetes Ingress
        |
        v
Kong Ingress Controller
        |
        v
Kong Gateway configuration
        |
        v
Kong Proxy
```

Esto permite declarar el routing mediante recursos Kubernetes en lugar de configurar manualmente cada ruta dentro de Kong.

Por ejemplo:

```text
Ingress
   |
   | /assistant
   v
app-searchia
```

El Ingress pertenece al namespace de la aplicación:

```text
applications
```

mientras que Kong está instalado en:

```text
kong
```

Esto demuestra que los componentes de Kong y las aplicaciones pueden mantenerse separados mediante namespaces.

---

# 7. Kubernetes Ingress

Se creó el recurso:

```text
app-searchia-ingress.yaml
```

Contenido:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-searchia
  namespace: applications
spec:
  ingressClassName: kong
  rules:
    - http:
        paths:
          - path: /assistant
            pathType: Prefix
            backend:
              service:
                name: app-searchia
                port:
                  number: 80
```

## Conceptos importantes

### `ingressClassName`

```yaml
ingressClassName: kong
```

Indica que este Ingress debe ser procesado por Kong Ingress Controller.

Esto permite que Kong distinga los recursos que debe administrar.

---

### `path`

```yaml
path: /assistant
```

Define el patrón de URL que será gestionado por Kong.

Por ejemplo:

```text
/assistant/search
```

coincide con:

```text
/assistant
```

porque se utiliza:

```yaml
pathType: Prefix
```

---

### Backend

```yaml
backend:
  service:
    name: app-searchia
    port:
      number: 80
```

Indica que las peticiones deben enviarse al Service:

```text
app-searchia:80
```

Kubernetes posteriormente realiza la traducción hacia el Pod:

```text
app-searchia:80
       |
       v
10.244.0.164:8080
```

---

# 8. Flujo completo de una petición

El flujo implementado es:

```text
Cliente
   |
   | POST /assistant/search
   |
   v
172.168.4.116
   |
   v
Kong Gateway
   |
   v
Kong Ingress Controller
   |
   v
Kubernetes Ingress
   |
   | /assistant
   v
app-searchia Service
   |
   | :80
   v
app-searchia Pod
   |
   | :8080
   v
Quarkus
   |
   +----------------------+
   |                      |
   v                      v
Azure AI Search      Azure OpenAI
```

---

# 9. ¿Por qué utilizar Kong Gateway?

Kubernetes Ingress proporciona principalmente una forma declarativa de definir routing HTTP.

Por ejemplo:

```text
/assistant -> app-searchia
/users     -> users-service
/orders    -> orders-service
```

Sin embargo, Kong añade capacidades propias de un **API Gateway**.

La arquitectura puede evolucionar hacia:

```text
                         Kong
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
          Routing    Authentication   Rate Limit
                           |
                           v
                         JWT
                           |
                           v
                      Authorization
                           |
                           v
                    Microservices
```

Entre las capacidades que se pueden probar posteriormente están:

- Authentication
- JWT
- API Key
- Authorization
- Rate Limiting
- CORS
- Request/Response transformation
- Logging
- Observability
- Plugins de seguridad

La principal ventaja es que estas responsabilidades pueden centralizarse en el API Gateway sin tener que implementarlas individualmente en cada microservicio.

---

### Kong Gateway

Es el componente que procesa el tráfico y puede aplicar políticas:

```text
Routing
Authentication
Authorization
Rate Limiting
CORS
Security
Logging
```

### Kong Ingress Controller

Es el componente que interpreta los recursos Kubernetes y los convierte en configuración de Kong:

```text
Ingress
   |
   v
Kong Ingress Controller
   |
   v
Kong Gateway
```

Por eso pueden utilizarse conjuntamente.

---

# 11. Integración con GitHub

El archivo:

```text
app-searchia-ingress.yaml
```

puede formar parte del repositorio Git y del proceso CI/CD.

Una estructura posible:

```text
app-searchIA/
|
+-- boot/
|
+-- ...
|
+-- k8s/
    |
    +-- deployment.yaml
    +-- service.yaml
    +-- app-searchia-ingress.yaml
```

El pipeline puede aplicar posteriormente:

```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/app-searchia-ingress.yaml
```

De esta forma, la configuración de routing de Kong queda versionada junto con el código de la aplicación.

---
## Fase 2 — API Key Authentication

kubectl apply -f app-searchia-api-key-secret.yaml
kubectl apply -f app-searchia-consumer.yaml
kubectl apply -f app-searchia-key-auth.yaml

### API Key Authentication

Componente	    Nombre
KongPlugin	    app-searchia-key-auth
KongConsumer	app-searchia-client
Secret	        app-searchia-api-key
API Key	        app-searchia-test-123
Header	        X-API-Key

kong-gateway-proxy = puerta de entrada
Ingress = reglas de entrada/routing
KongPlugin = seguridad aplicada a esas reglas
KongConsumer = quién está consumiendo
Secret = credencial

### Creamos un KongPlugin

Este recurso le dice a Kong: "Quiero utilizar el plugin key-auth y quiero que la API Key venga en el header X-API-Key."

Define cómo autenticar.

### Creamos un Consumer

El Consumer representa quién está consumiendo nuestra API. Puedes imaginarlo como un cliente registrado en Kong.

Define quién consume.

Por ejemplo, podríamos tener:

```text
app-searchia-client
mobile-app
web-app
partner-company
external-system
```

Cada Consumer podría tener diferentes credenciales.

    kubectl get kongconsumer -n applications
    kubectl get kongconsumer app-searchia-client -n applications

### Credential

Define con qué credencial se autentica.

El Consumer tiene:

```text
credentials:
- app-searchia-api-key
```

Esto significa: El Consumer app-searchia-client utiliza la credencial app-searchia-api-key.

    kubectl get kongplugin -A

### Secret de Kubernetes

La API Key se almacena en un Secret. Es donde Kubernetes almacena esa credencial.

¿Cómo sabe Kong qué API proteger?

El plugin se asocia al Ingress mediante:

```yaml
annotations:
  konghq.com/plugins: app-searchia-key-auth
```


## Fase 3 — JWT Authentication

Keycloak será nuestro Identity Provider (IdP) y Kong será el API Gateway que valida el token. Keycloak expone, entre otros, el endpoint OIDC de descubrimiento y el endpoint de claves públicas/JWK que permiten validar los JWT.

## FASE 1 — Instalar Keycloak en AKS

Kong acceda internamente a Keycloak, sin necesidad de exponer Keycloak a Internet.

```yaml
    kubectl create namespace keycloak
    kubectl apply -k 'github.com/keycloak/keycloak-k8s-resources/kubernetes?ref=26.7.2'
    kubectl get pods -n keycloak
    kubectl get deployment -n keycloak

    kubectl apply -f k8s/keycloak/keycloak.yaml
    kubectl get pods -n keycloak
    kubectl get keycloak -n keycloak
```
### Crear un Ingress para Keycloak

Para un laboratorio podemos utilizar un hostname sencillo: keycloak.172.168.4.116.nip.io

```yaml
    kubectl apply -f k8s/keycloak/keycloak-ingress.yaml
```
 Prueba desde tu pc: http://keycloak.172.168.4.116.nip.io

 get user and password:

    temp-admin

```yaml
    kubectl get secret -n keycloak keycloak-initial-admin \
    -o jsonpath='{.data.username}' | base64 -d
    echo
    
    kubectl get secret -n keycloak keycloak-initial-admin \
    -o jsonpath='{.data.password}' | base64 -d
    echo
```
Debemos de crear datos:

    Realm
    microservices
    
    Users
    └── luis
    
    Clients
    └── app-searchia

curl -X POST \
http://keycloak.172.168.4.116.nip.io/realms/microservices/protocol/openid-connect/token \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "client_id=app-searchia" \
-d "username=lgomezs" \
-d "password=jueves1514" \
-d "grant_type=password"


### Crear la credencial JWT de Kong , KongConsumer y Crear el plugin JWT

```yaml
kubectl apply -f k8s/app-searchia-jwt-credential.yaml
kubectl apply -f k8s/app-searchia-jwt-consumer.yaml
kubectl apply -f k8s/app-searchia-jwt-plugin.yaml
```

Validamos:

  ```yaml
kubectl get secret app-searchia-jwt-credential \
-n applications \
--show-labels

kubectl get kongconsumer app-searchia-jwt-client \
-n applications

kubectl get kongplugin app-searchia-jwt \
  -n applications
```

Flujo:

```text
Cliente
   |
   | Authorization: Bearer <JWT>
   v
Kong
   |
   | Validación JWT
   v
app-searchia
```

Kong rechazará tokens inválidos antes de que la petición llegue al microservicio.

---

## Fase 4 — Rate Limiting

Ejemplo:

```text
10 requests / minute
```

Cuando se supere el límite:

```text
HTTP 429 Too Many Requests
```

---

## Fase 5 — CORS

Permitir que una aplicación frontend pueda consumir:

```text
Frontend
   |
   v
Kong
   |
   v
app-searchia
```

controlando los orígenes permitidos.

---
