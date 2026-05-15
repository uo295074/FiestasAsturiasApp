package es.uniovi.imovil.fiestasasturias.model

// envoltorio raíz de la respuesta remota.
// retrofit entra por aquí y desde "article" sacamos cada FiestaDto.
data class FiestaResponse (
    val articles: Articles
)
data class Articles(
    val article: List<FiestaDto>
)
