# Guía Completa de Configuración Resilience4j

## Introducción

Esta guía proporciona una referencia completa para configurar Resilience4j en diferentes escenarios de servicios, desde críticos hasta no críticos, considerando factores como frecuencia de uso, latencia de servicios externos y requisitos de rendimiento.

## Circuit Breaker Configuration

| Parámetro | Valor Actual | Justificación | Servicio Crítico | Servicio No Crítico | Alta Frecuencia | Baja Frecuencia | Servicio Externo Lento | Servicio Interno Rápido |
|-----------|--------------|---------------|-------------------|---------------------|-----------------|----------------|------------------------|-------------------------|
| **failureRateThreshold** | Auth: `40%`<br>Business: `50%` | Auth necesita fallar rápido para no bloquear flujos críticos. Business puede tolerar más fallos antes de abrir circuito | `20-30%`<br>*Falla muy rápido* | `60-70%`<br>*Tolerante a fallos* | `30-40%`<br>*Detecta problemas rápido en alto volumen* | `50-60%`<br>*Más tolerante debido a menor muestra* | `40-50%`<br>*Compensa latencia natural* | `20-30%`<br>*Debe ser muy responsivo* |
| **waitDurationInOpenState** | Auth: `15s`<br>Business: `20s` | Auth debe recuperarse rápido para no afectar UX. Business puede esperar más tiempo para estabilizarse | `5-10s`<br>*Recuperación rápida* | `30-60s`<br>*Mayor tiempo de recuperación* | `10-15s`<br>*Balance entre volumen y recuperación* | `30-45s`<br>*Permite recuperación completa* | `30-60s`<br>*Tiempo para que servicio remoto se recupere* | `5-10s`<br>*Recuperación rápida esperada* |
| **slidingWindowSize** | Auth: `8`<br>Business: `10` | Ventana pequeña para detectar fallos rápidamente en auth. Business usa ventana estándar | `5-8`<br>*Detección ultra rápida* | `15-20`<br>*Evaluación más estable* | `20-50`<br>*Muestra grande para estadísticas precisas* | `5-10`<br>*Ventana pequeña por bajo volumen* | `10-20`<br>*Balance para compensar latencia* | `5-10`<br>*Detección rápida* |
| **permittedNumberOfCallsInHalfOpenState** | Auth: `3`<br>Business: `3` | Número conservador para probar recuperación sin sobrecargar | `2-3`<br>*Pruebas mínimas* | `5-10`<br>*Más pruebas para confirmar recuperación* | `3-5`<br>*Balance para volumen* | `2-4`<br>*Pocas pruebas por bajo volumen* | `3-6`<br>*Más pruebas para compensar latencia* | `2-3`<br>*Pruebas rápidas* |
| **slowCallRateThreshold** | `50%` (recomendado) | Detecta degradación de rendimiento antes de fallos completos | `30-40%`<br>*Muy sensible a latencia* | `60-70%`<br>*Tolerante a latencia* | `40-50%`<br>*Balance para alto volumen* | `50-60%`<br>*Más tolerante* | `60-70%`<br>*Compensa latencia natural* | `20-30%`<br>*Muy sensible* |
| **slowCallDurationThreshold** | Auth: `2s`<br>Business: `5s` | Define qué se considera "lento" según el contexto del servicio | `1-2s`<br>*Muy estricto* | `5-10s`<br>*Tolerante* | `2-3s`<br>*Balance para volumen* | `3-8s`<br>*Más tolerante* | `8-15s`<br>*Acomoda latencia de red* | `500ms-1s`<br>*Muy estricto* |

## Retry Configuration

| Parámetro | Valor Actual | Justificación | Servicio Crítico | Servicio No Crítico | Alta Frecuencia | Baja Frecuencia | Servicio Externo Lento | Servicio Interno Rápido |
|-----------|--------------|---------------|-------------------|---------------------|-----------------|----------------|------------------------|-------------------------|
| **maxAttempts** | Auth: `2`<br>Business: `3` | Auth evita saturar servidor de autenticación. Business intenta más veces para completar operaciones importantes | `1-2`<br>*Mínimos reintentos para evitar cascada* | `3-5`<br>*Más reintentos para maximizar éxito* | `2-3`<br>*Pocos reintentos para no amplificar carga* | `3-5`<br>*Más reintentos por menor impacto en volumen* | `3-4`<br>*Más intentos para superar problemas de red* | `1-2`<br>*Fallos rápidos esperados* |
| **waitDuration** | Auth: `1s`<br>Business: `1.5s` | Tiempo base conservador para permitir recuperación sin ser muy agresivo | `500ms-1s`<br>*Recuperación rápida* | `2-3s`<br>*Más tiempo entre intentos* | `500ms-1s`<br>*Reintentos rápidos* | `2-5s`<br>*Más espaciado por menor urgencia* | `3-5s`<br>*Tiempo para recuperación de red* | `200-500ms`<br>*Reintentos muy rápidos* |
| **enableExponentialBackoff** | `true` | Previene el "retry storm" y permite recuperación gradual del servicio | `true`<br>*Esencial para servicios críticos* | `true`<br>*Recomendado siempre* | `true`<br>*Crítico para alto volumen* | `false/true`<br>*Opcional por bajo volumen* | `true`<br>*Esencial para servicios externos* | `true`<br>*Recomendado* |
| **exponentialBackoffMultiplier** | Auth: `2`<br>Business: `1.5` | Auth usa multiplicador estándar. Business más agresivo para balancear velocidad y espaciado | `1.5-2`<br>*Balance entre velocidad y protección* | `2-3`<br>*Más espaciado* | `1.5-2`<br>*Progresión moderada* | `2-3`<br>*Mayor espaciado* | `2-3`<br>*Compensar latencia de red* | `1.2-1.5`<br>*Progresión suave* |
| **randomizationFactor** | `0.5` (recomendado) | Añade jitter para evitar reintentos sincronizados | `0.3-0.5`<br>*Jitter moderado* | `0.5-0.7`<br>*Más jitter* | `0.4-0.6`<br>*Jitter para distribuir carga* | `0.2-0.4`<br>*Menos jitter por bajo volumen* | `0.5-0.8`<br>*Más jitter para red* | `0.2-0.4`<br>*Jitter ligero* |

## Time Limiter Configuration

| Parámetro | Valor Actual | Justificación | Servicio Crítico | Servicio No Crítico | Alta Frecuencia | Baja Frecuencia | Servicio Externo Lento | Servicio Interno Rápido |
|-----------|--------------|---------------|-------------------|---------------------|-----------------|----------------|------------------------|-------------------------|
| **timeoutDuration** | Auth: `3s`<br>Business: `8s` | Auth debe ser rápido para UX. Business permite operaciones más complejas | `1-2s`<br>*Timeout muy agresivo* | `10-30s`<br>*Timeout generoso* | `2-5s`<br>*Balance para throughput* | `10-15s`<br>*Más tiempo por menor presión* | `15-30s`<br>*Acomoda latencia de red* | `500ms-2s`<br>*Muy rápido* |
| **cancelRunningFuture** | `true` | Libera recursos inmediatamente al alcanzar el timeout | `true`<br>*Esencial para liberar recursos* | `true`<br>*Recomendado siempre* | `true`<br>*Crítico para throughput* | `true`<br>*Buena práctica* | `true`<br>*Importante para servicios remotos* | `true`<br>*Esencial para velocidad* |

## Configuraciones Adicionales por Escenario

### Bulkhead Configuration

| Parámetro | Servicio Crítico | Servicio No Crítico | Alta Frecuencia | Baja Frecuencia | Servicio Externo Lento | Servicio Interno Rápido |
|-----------|-------------------|---------------------|-----------------|----------------|------------------------|-------------------------|
| **maxConcurrentCalls** | `5-10`<br>*Pocos hilos para evitar saturación* | `20-50`<br>*Más hilos disponibles* | `30-100`<br>*Muchos hilos para volumen* | `5-15`<br>*Pocos hilos necesarios* | `10-20`<br>*Moderado para compensar latencia* | `50-100`<br>*Muchos hilos por velocidad* |
| **maxWaitDuration** | `100-500ms`<br>*Espera mínima* | `1-5s`<br>*Espera más larga* | `200-1000ms`<br>*Balance para throughput* | `1-3s`<br>*Más tiempo por menor urgencia* | `1-3s`<br>*Compensa latencia* | `50-200ms`<br>*Espera muy corta* |

### Rate Limiter Configuration

| Parámetro | Servicio Crítico | Servicio No Crítico | Alta Frecuencia | Baja Frecuencia | Servicio Externo Lento | Servicio Interno Rápido |
|-----------|-------------------|---------------------|-----------------|----------------|------------------------|-------------------------|
| **limitForPeriod** | `10-50/s`<br>*Límite conservador* | `100-500/s`<br>*Límite generoso* | `1000-5000/s`<br>*Límite alto para volumen* | `10-100/s`<br>*Límite bajo* | `50-200/s`<br>*Compensar latencia* | `500-2000/s`<br>*Límite alto* |
| **limitRefreshPeriod** | `1s`<br>*Refresh estándar* | `1-5s`<br>*Refresh más lento* | `500ms-1s`<br>*Refresh rápido* | `5-10s`<br>*Refresh lento* | `2-5s`<br>*Compensar latencia* | `200ms-1s`<br>*Refresh muy rápido* |
| **timeoutDuration** | `100-500ms`<br>*Timeout corto* | `1-3s`<br>*Timeout largo* | `200-1000ms`<br>*Balance* | `1-5s`<br>*Timeout generoso* | `1-3s`<br>*Compensa latencia* | `50-200ms`<br>*Timeout muy corto* |

## Ejemplos de Configuración Completa por Escenario

### 🔴 Servicio Crítico (Autenticación, Pagos, Core Business)

```properties
# Circuit Breaker - Configuración muy sensible
resilience4j.circuitbreaker.instances.criticalService.failureRateThreshold=25
resilience4j.circuitbreaker.instances.criticalService.waitDurationInOpenState=10000
resilience4j.circuitbreaker.instances.criticalService.slidingWindowSize=6
resilience4j.circuitbreaker.instances.criticalService.permittedNumberOfCallsInHalfOpenState=2
resilience4j.circuitbreaker.instances.criticalService.slowCallRateThreshold=35
resilience4j.circuitbreaker.instances.criticalService.slowCallDurationThreshold=1500

# Retry - Mínimos reintentos para evitar cascada
resilience4j.retry.instances.criticalService.maxAttempts=2
resilience4j.retry.instances.criticalService.waitDuration=800
resilience4j.retry.instances.criticalService.enableExponentialBackoff=true
resilience4j.retry.instances.criticalService.exponentialBackoffMultiplier=1.8
resilience4j.retry.instances.criticalService.randomizationFactor=0.4

# Time Limiter - Timeout agresivo
resilience4j.timelimiter.instances.criticalService.timeoutDuration=2s
resilience4j.timelimiter.instances.criticalService.cancelRunningFuture=true

# Bulkhead - Limitación estricta de recursos
resilience4j.bulkhead.instances.criticalService.maxConcurrentCalls=8
resilience4j.bulkhead.instances.criticalService.maxWaitDuration=300

# Rate Limiter - Límite conservador
resilience4j.ratelimiter.instances.criticalService.limitForPeriod=30
resilience4j.ratelimiter.instances.criticalService.limitRefreshPeriod=1s
resilience4j.ratelimiter.instances.criticalService.timeoutDuration=200
```

### 🟡 Servicio No Crítico (Notificaciones, Analytics, Logging)

```properties
# Circuit Breaker - Configuración tolerante
resilience4j.circuitbreaker.instances.nonCriticalService.failureRateThreshold=65
resilience4j.circuitbreaker.instances.nonCriticalService.waitDurationInOpenState=45000
resilience4j.circuitbreaker.instances.nonCriticalService.slidingWindowSize=20
resilience4j.circuitbreaker.instances.nonCriticalService.permittedNumberOfCallsInHalfOpenState=8
resilience4j.circuitbreaker.instances.nonCriticalService.slowCallRateThreshold=70
resilience4j.circuitbreaker.instances.nonCriticalService.slowCallDurationThreshold=8000

# Retry - Más reintentos para maximizar éxito
resilience4j.retry.instances.nonCriticalService.maxAttempts=4
resilience4j.retry.instances.nonCriticalService.waitDuration=2500
resilience4j.retry.instances.nonCriticalService.enableExponentialBackoff=true
resilience4j.retry.instances.nonCriticalService.exponentialBackoffMultiplier=2.5
resilience4j.retry.instances.nonCriticalService.randomizationFactor=0.6

# Time Limiter - Timeout generoso
resilience4j.timelimiter.instances.nonCriticalService.timeoutDuration=15s
resilience4j.timelimiter.instances.nonCriticalService.cancelRunningFuture=true

# Bulkhead - Más recursos disponibles
resilience4j.bulkhead.instances.nonCriticalService.maxConcurrentCalls=35
resilience4j.bulkhead.instances.nonCriticalService.maxWaitDuration=3000

# Rate Limiter - Límite generoso
resilience4j.ratelimiter.instances.nonCriticalService.limitForPeriod=300
resilience4j.ratelimiter.instances.nonCriticalService.limitRefreshPeriod=2s
resilience4j.ratelimiter.instances.nonCriticalService.timeoutDuration=2000
```

### 🔥 Alta Frecuencia (APIs públicas, Microservicios core)

```properties
# Circuit Breaker - Balance para alto volumen
resilience4j.circuitbreaker.instances.highFrequencyService.failureRateThreshold=35
resilience4j.circuitbreaker.instances.highFrequencyService.waitDurationInOpenState=12000
resilience4j.circuitbreaker.instances.highFrequencyService.slidingWindowSize=30
resilience4j.circuitbreaker.instances.highFrequencyService.permittedNumberOfCallsInHalfOpenState=5
resilience4j.circuitbreaker.instances.highFrequencyService.slowCallRateThreshold=45
resilience4j.circuitbreaker.instances.highFrequencyService.slowCallDurationThreshold=2500

# Retry - Pocos reintentos para no amplificar carga
resilience4j.retry.instances.highFrequencyService.maxAttempts=2
resilience4j.retry.instances.highFrequencyService.waitDuration=600
resilience4j.retry.instances.highFrequencyService.enableExponentialBackoff=true
resilience4j.retry.instances.highFrequencyService.exponentialBackoffMultiplier=1.6
resilience4j.retry.instances.highFrequencyService.randomizationFactor=0.5

# Time Limiter - Balance para throughput
resilience4j.timelimiter.instances.highFrequencyService.timeoutDuration=3s
resilience4j.timelimiter.instances.highFrequencyService.cancelRunningFuture=true

# Bulkhead - Muchos hilos para volumen
resilience4j.bulkhead.instances.highFrequencyService.maxConcurrentCalls=75
resilience4j.bulkhead.instances.highFrequencyService.maxWaitDuration=800

# Rate Limiter - Límite alto para volumen
resilience4j.ratelimiter.instances.highFrequencyService.limitForPeriod=2500
resilience4j.ratelimiter.instances.highFrequencyService.limitRefreshPeriod=1s
resilience4j.ratelimiter.instances.highFrequencyService.timeoutDuration=500
```

### 🐌 Servicio Externo Lento (APIs de terceros, Servicios legados)

```properties
# Circuit Breaker - Tolerante a latencia natural
resilience4j.circuitbreaker.instances.externalSlowService.failureRateThreshold=45
resilience4j.circuitbreaker.instances.externalSlowService.waitDurationInOpenState=60000
resilience4j.circuitbreaker.instances.externalSlowService.slidingWindowSize=15
resilience4j.circuitbreaker.instances.externalSlowService.permittedNumberOfCallsInHalfOpenState=4
resilience4j.circuitbreaker.instances.externalSlowService.slowCallRateThreshold=65
resilience4j.circuitbreaker.instances.externalSlowService.slowCallDurationThreshold=12000

# Retry - Más intentos para superar problemas de red
resilience4j.retry.instances.externalSlowService.maxAttempts=3
resilience4j.retry.instances.externalSlowService.waitDuration=4000
resilience4j.retry.instances.externalSlowService.enableExponentialBackoff=true
resilience4j.retry.instances.externalSlowService.exponentialBackoffMultiplier=2.2
resilience4j.retry.instances.externalSlowService.randomizationFactor=0.7

# Time Limiter - Timeout largo para latencia de red
resilience4j.timelimiter.instances.externalSlowService.timeoutDuration=20s
resilience4j.timelimiter.instances.externalSlowService.cancelRunningFuture=true

# Bulkhead - Moderado para compensar latencia
resilience4j.bulkhead.instances.externalSlowService.maxConcurrentCalls=15
resilience4j.bulkhead.instances.externalSlowService.maxWaitDuration=2500

# Rate Limiter - Límite moderado para compensar latencia
resilience4j.ratelimiter.instances.externalSlowService.limitForPeriod=120
resilience4j.ratelimiter.instances.externalSlowService.limitRefreshPeriod=3s
resilience4j.ratelimiter.instances.externalSlowService.timeoutDuration=2000
```

### ⚡ Servicio Interno Rápido (Caché, Base de datos local)

```properties
# Circuit Breaker - Muy sensible a cualquier degradación
resilience4j.circuitbreaker.instances.internalFastService.failureRateThreshold=25
resilience4j.circuitbreaker.instances.internalFastService.waitDurationInOpenState=8000
resilience4j.circuitbreaker.instances.internalFastService.slidingWindowSize=8
resilience4j.circuitbreaker.instances.internalFastService.permittedNumberOfCallsInHalfOpenState=2
resilience4j.circuitbreaker.instances.internalFastService.slowCallRateThreshold=25
resilience4j.circuitbreaker.instances.internalFastService.slowCallDurationThreshold=800

# Retry - Fallos rápidos esperados
resilience4j.retry.instances.internalFastService.maxAttempts=2
resilience4j.retry.instances.internalFastService.waitDuration=300
resilience4j.retry.instances.internalFastService.enableExponentialBackoff=true
resilience4j.retry.instances.internalFastService.exponentialBackoffMultiplier=1.3
resilience4j.retry.instances.internalFastService.randomizationFactor=0.3

# Time Limiter - Timeout muy agresivo
resilience4j.timelimiter.instances.internalFastService.timeoutDuration=1s
resilience4j.timelimiter.instances.internalFastService.cancelRunningFuture=true

# Bulkhead - Muchos hilos por velocidad esperada
resilience4j.bulkhead.instances.internalFastService.maxConcurrentCalls=80
resilience4j.bulkhead.instances.internalFastService.maxWaitDuration=150

# Rate Limiter - Límite muy alto
resilience4j.ratelimiter.instances.internalFastService.limitForPeriod=1500
resilience4j.ratelimiter.instances.internalFastService.limitRefreshPeriod=500ms
resilience4j.ratelimiter.instances.internalFastService.timeoutDuration=100
```

## Configuración de Excepciones

### Excepciones a Ignorar (No reintentables)

```properties
# Para todos los servicios - Excepciones que no deben reintentarse
resilience4j.retry.instances.service.ignoreExceptions=\
  java.lang.IllegalArgumentException,\
  javax.validation.ValidationException,\
  org.springframework.security.access.AccessDeniedException,\
  org.springframework.web.client.HttpClientErrorException.BadRequest,\
  org.springframework.web.client.HttpClientErrorException.Unauthorized,\
  org.springframework.web.client.HttpClientErrorException.Forbidden,\
  org.springframework.web.client.HttpClientErrorException.NotFound

# Para servicios externos - Excepciones específicas de red a reintentar
resilience4j.retry.instances.externalService.retryExceptions=\
  java.io.IOException,\
  java.net.SocketTimeoutException,\
  java.net.ConnectException,\
  org.springframework.web.client.ResourceAccessException,\
  org.springframework.web.client.HttpServerErrorException
```

## Monitoreo y Métricas

### Configuración de Actuator para Monitoreo

```properties
# Habilitar endpoints de monitoreo
management.endpoints.web.exposure.include=health,info,metrics,prometheus,circuitbreakers,retries
management.endpoint.health.show-details=always
management.endpoint.circuitbreakers.enabled=true
management.endpoint.retries.enabled=true

# Métricas específicas de Resilience4j
management.metrics.export.prometheus.enabled=true
resilience4j.circuitbreaker.instances.*.registerHealthIndicator=true
resilience4j.retry.instances.*.registerHealthIndicator=true
resilience4j.bulkhead.instances.*.registerHealthIndicator=true
resilience4j.ratelimiter.instances.*.registerHealthIndicator=true
```

## Mejores Prácticas y Recomendaciones

### 🎯 Principios Generales

1. **Comienza Conservador**: Inicia con valores más restrictivos y relaja gradualmente basándote en métricas
2. **Mide Todo**: Configura métricas exhaustivas para tomar decisiones basadas en datos
3. **Prueba en Carga**: Valida configuraciones bajo condiciones de carga realistas
4. **Documenta Decisiones**: Mantén documentación clara de por qué elegiste valores específicos

### 🔧 Configuración por Fases

#### Fase 1 - Configuración Inicial (Conservadora)
```properties
# Valores seguros para comenzar
failureRateThreshold=30
waitDurationInOpenState=20s
maxAttempts=2
timeoutDuration=5s
```

#### Fase 2 - Optimización basada en métricas
- Analizar tasas de fallo reales
- Ajustar thresholds basándote en percentiles de latencia
- Optimizar timeouts según P95/P99 de respuestas

#### Fase 3 - Configuración Avanzada
- Implementar configuraciones específicas por endpoint
- Añadir configuraciones dinámicas basadas en carga
- Implementar circuit breakers en cascada

### ⚠️ Antipatrones a Evitar

1. **Retry Storm**: Demasiados reintentos que saturan el servicio
2. **Timeout Inconsistente**: Timeouts más largos que los circuit breaker waits
3. **Window Size Inadecuado**: Ventanas muy pequeñas que causan falsos positivos
4. **Fallback Costoso**: Fallbacks que consumen más recursos que la operación original

### 📊 Métricas Clave a Monitorear

- **Circuit Breaker State**: % tiempo en cada estado (closed/open/half-open)
- **Failure Rate**: Tasa de fallos real vs threshold configurado
- **Latency Percentiles**: P50, P95, P99 de tiempo de respuesta
- **Retry Success Rate**: % de reintentos que terminan exitosamente
- **Timeout Rate**: % de operaciones que exceden el timeout configurado

### 🔄 Ciclo de Mejora Continua

1. **Monitoreo**: Recopilar métricas durante 1-2 semanas
2. **Análisis**: Identificar patrones de fallo y latencia
3. **Ajuste**: Modificar configuraciones basándote en datos
4. **Validación**: Confirmar mejoras en próximas 1-2 semanas
5. **Repetir**: Ciclo continuo de optimización

## Troubleshooting Común

### Problema: Circuit Breaker se abre muy frecuentemente
**Soluciones**:
- Aumentar `failureRateThreshold`
- Aumentar `slidingWindowSize`
- Revisar si las excepciones configuradas son correctas

### Problema: Timeouts muy frecuentes
**Soluciones**:
- Aumentar `timeoutDuration`
- Revisar latencia real del servicio (P95/P99)
- Verificar si hay bottlenecks de red o infraestructura

### Problema: Reintentos no están funcionando
**Soluciones**:
- Verificar que las excepciones estén en `retryExceptions`
- Confirmar que no están en `ignoreExceptions`
- Revisar si el circuit breaker está interfiriendo

### Problema: Alto consumo de recursos
**Soluciones**:
- Implementar bulkhead con `maxConcurrentCalls` más bajo
- Reducir `maxAttempts` en retry
- Implementar rate limiting

## Conclusión

La configuración de Resilience4j debe ser un proceso iterativo basado en el comportamiento real de tu sistema. Comienza con configuraciones conservadoras, mide todo, y ajusta gradualmente basándote en datos reales. Recuerda que la configuración óptima varía significativamente según el contexto específico de cada servicio y su criticidad para el negocio.