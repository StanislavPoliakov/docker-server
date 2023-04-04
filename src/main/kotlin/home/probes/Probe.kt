package home.probes

import kotlinx.serialization.Serializable

object Probe {
    var liveness: ProbeStatus = ProbeStatus.SUCCESS
    var readiness: ProbeStatus = ProbeStatus.SUCCESS
}

enum class ProbeStatus {
    SUCCESS, FAILED
}

@Serializable
data class LivenessProbe(val liveness: ProbeStatus)

@Serializable
data class ReadinessProbe(val readiness: ProbeStatus)

