package es.uniovi.imovil.fiestasasturias.model

data class FiestaResponse (
    val articles: Articles
)
data class Articles(
    val article: List<FiestaDto>
)