package com.sonara.app.ui.theme

enum class SonaraFont(val displayName: String, val googleFontName: String?) {
    SYSTEM_DEFAULT("System Default", null),
    INTER("Inter", "Inter"),
    POPPINS("Poppins", "Poppins"),
    DM_SANS("DM Sans", "DM Sans"),
    FIGTREE("Figtree", "Figtree"),
    MANROPE("Manrope", "Manrope"),
    MONTSERRAT("Montserrat", "Montserrat"),
    OPEN_SANS("Open Sans", "Open Sans"),
    OUTFIT("Outfit", "Outfit"),
    QUICKSAND("Quicksand", "Quicksand"),
    NUNITO("Nunito", "Nunito"),
    RALEWAY("Raleway", "Raleway"),
    SPACE_GROTESK("Space Grotesk", "Space Grotesk"),
    PLUS_JAKARTA("Plus Jakarta Sans", "Plus Jakarta Sans"),
    LEXEND("Lexend", "Lexend"),
    SORA("Sora", "Sora"),
    JOSEFIN("Josefin Sans", "Josefin Sans"),
    URBANIST("Urbanist", "Urbanist"),
    LATO("Lato", "Lato"),
    NOTO_SANS("Noto Sans", "Noto Sans"),
    RUBIK("Rubik", "Rubik"),
    WORK_SANS("Work Sans", "Work Sans"),
    MULISH("Mulish", "Mulish"),
    KARLA("Karla", "Karla"),
    IBM_PLEX_SANS("IBM Plex Sans", "IBM Plex Sans"),
    JOST("Jost", "Jost"),
    CABIN("Cabin", "Cabin"),
    BARLOW("Barlow", "Barlow"),
    EXO_2("Exo 2", "Exo 2"),
    MAVEN_PRO("Maven Pro", "Maven Pro");

    companion object {
        fun fromId(id: String) = entries.find { it.name == id } ?: INTER
    }
}
