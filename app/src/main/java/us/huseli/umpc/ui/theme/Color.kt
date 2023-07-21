package us.huseli.umpc.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

interface UmpcColorScheme {
    val Background: Color
    val Error: Color
    val ErrorContainer: Color
    val InverseOnSurface: Color
    val InversePrimary: Color
    val InverseSurface: Color
    val OnBackground: Color
    val OnError: Color
    val OnErrorContainer: Color
    val OnPrimary: Color
    val OnPrimaryContainer: Color
    val OnPrimaryFixed: Color
    val OnPrimaryFixedVariant: Color
    val OnSecondary: Color
    val OnSecondaryContainer: Color
    val OnSecondaryFixed: Color
    val OnSecondaryFixedVariant: Color
    val OnSurface: Color
    val OnSurfaceVariant: Color
    val OnTertiary: Color
    val OnTertiaryContainer: Color
    val OnTertiaryFixed: Color
    val OnTertiaryFixedVariant: Color
    val Outline: Color
    val OutlineVariant: Color
    val Primary: Color
    val PrimaryContainer: Color
    val PrimaryFixed: Color
    val PrimaryFixedDim: Color
    val Scrim: Color
    val Secondary: Color
    val SecondaryContainer: Color
    val SecondaryFixed: Color
    val SecondaryFixedDim: Color
    val Shadow: Color
    val Surface: Color
    val SurfaceBright: Color
    val SurfaceContainer: Color
    val SurfaceContainerHigh: Color
    val SurfaceContainerHighest: Color
    val SurfaceContainerLow: Color
    val SurfaceContainerLowest: Color
    val SurfaceDim: Color
    val SurfaceTint: Color
    val SurfaceVariant: Color
    val Tertiary: Color
    val TertiaryContainer: Color
    val TertiaryFixed: Color
    val TertiaryFixedDim: Color

    val Brown: Color
    val Purple: Color
    val Cerulean: Color
    val Gray: Color
    val Pink: Color
    val Blue: Color
    val Red: Color
    val Yellow: Color
    val Green: Color
    val Teal: Color
    val Orange: Color
}

object UmpcColorDark : UmpcColorScheme {
    override val Background = Color(0xFF191C1D)
    override val Error = Color(0xFFFFB4AB)
    override val ErrorContainer = Color(0xFF93000A)
    override val InverseOnSurface = Color(0xFF191C1D)
    override val InversePrimary = Color(0xFF00658F)
    override val InverseSurface = Color(0xFFE1E3E3)
    override val OnBackground = Color(0xFFE1E3E3)
    override val OnError = Color(0xFF690005)
    override val OnErrorContainer = Color(0xFFFFDAD6)
    override val OnPrimary = Color(0xFF00344C)
    override val OnPrimaryContainer = Color(0xFFC7E7FF)
    override val OnPrimaryFixed = Color(0xFF001E2E)
    override val OnPrimaryFixedVariant = Color(0xFF004C6C)
    override val OnSecondary = Color(0xFF21323E)
    override val OnSecondaryContainer = Color(0xFFD2E5F5)
    override val OnSecondaryFixed = Color(0xFF0B1D29)
    override val OnSecondaryFixedVariant = Color(0xFF374955)
    override val OnSurface = Color(0xFFC4C7C7)
    override val OnSurfaceVariant = Color(0xFFBFC8CA)
    override val OnTertiary = Color(0xFF293500)
    override val OnTertiaryContainer = Color(0xFFD4ED7F)
    override val OnTertiaryFixed = Color(0xFF171E00)
    override val OnTertiaryFixedVariant = Color(0xFF3D4D00)
    override val Outline = Color(0xFF899294)
    override val OutlineVariant = Color(0xFF3F484A)
    override val Primary = Color(0xFF85CFFF)
    override val PrimaryContainer = Color(0xFF004C6C)
    override val PrimaryFixed = Color(0xFFC7E7FF)
    override val PrimaryFixedDim = Color(0xFF85CFFF)
    override val Scrim = Color(0xFF000000)
    override val Secondary = Color(0xFFB6C9D8)
    override val SecondaryContainer = Color(0xFF374955)
    override val SecondaryFixed = Color(0xFFD2E5F5)
    override val SecondaryFixedDim = Color(0xFFB6C9D8)
    override val Shadow = Color(0xFF000000)
    override val Surface = Color(0xFF101415)
    override val SurfaceBright = Color(0xFF363A3A)
    override val SurfaceContainer = Color(0xFF1D2021)
    override val SurfaceContainerHigh = Color(0xFF272B2B)
    override val SurfaceContainerHighest = Color(0xFF323536)
    override val SurfaceContainerLow = Color(0xFF191C1D)
    override val SurfaceContainerLowest = Color(0xFF0B0F0F)
    override val SurfaceDim = Color(0xFF101415)
    override val SurfaceTint = Color(0xFF85CFFF)
    override val SurfaceVariant = Color(0xFF3F484A)
    override val Tertiary = Color(0xFFB8D166)
    override val TertiaryContainer = Color(0xFF3D4D00)
    override val TertiaryFixed = Color(0xFFD4ED7F)
    override val TertiaryFixedDim = Color(0xFFB8D166)

    override val Brown = Color(0xff4b443a)
    override val Purple = Color(0xff472e5b)
    override val Cerulean = Color(0xff284255)
    override val Gray = Color(0xff232427)
    override val Pink = Color(0xff6c394f)
    override val Blue = Color(0xff256377)
    override val Red = Color(0xff77172e)
    override val Yellow = Color(0xff7c4a03)
    override val Green = Color(0xff264d3b)
    override val Teal = Color(0xff0c625d)
    override val Orange = Color(0xff692b17)
}

object UmpcColorLight : UmpcColorScheme {
    override val Background = Color(0xFFFAFDFD)
    override val Error = Color(0xFFBA1A1A)
    override val ErrorContainer = Color(0xFFFFDAD6)
    override val InverseOnSurface = Color(0xFFEFF1F1)
    override val InversePrimary = Color(0xFF85CFFF)
    override val InverseSurface = Color(0xFF2E3132)
    override val OnBackground = Color(0xFF191C1D)
    override val OnError = Color(0xFFFFFFFF)
    override val OnErrorContainer = Color(0xFF410002)
    override val OnPrimary = Color(0xFFFFFFFF)
    override val OnPrimaryContainer = Color(0xFF001E2E)
    override val OnPrimaryFixed = Color(0xFF001E2E)
    override val OnPrimaryFixedVariant = Color(0xFF004C6C)
    override val OnSecondary = Color(0xFFFFFFFF)
    override val OnSecondaryContainer = Color(0xFF0B1D29)
    override val OnSecondaryFixed = Color(0xFF0B1D29)
    override val OnSecondaryFixedVariant = Color(0xFF374955)
    override val OnSurface = Color(0xFF191C1D)
    override val OnSurfaceVariant = Color(0xFF3F484A)
    override val OnTertiary = Color(0xFFFFFFFF)
    override val OnTertiaryContainer = Color(0xFF171E00)
    override val OnTertiaryFixed = Color(0xFF171E00)
    override val OnTertiaryFixedVariant = Color(0xFF3D4D00)
    override val Outline = Color(0xFF6F797A)
    override val OutlineVariant = Color(0xFFBFC8CA)
    override val Primary = Color(0xFF00658F)
    override val PrimaryContainer = Color(0xFFC7E7FF)
    override val PrimaryFixed = Color(0xFFC7E7FF)
    override val PrimaryFixedDim = Color(0xFF85CFFF)
    override val Scrim = Color(0xFF000000)
    override val Secondary = Color(0xFF4F616E)
    override val SecondaryContainer = Color(0xFFD2E5F5)
    override val SecondaryFixed = Color(0xFFD2E5F5)
    override val SecondaryFixedDim = Color(0xFFB6C9D8)
    override val Shadow = Color(0xFF000000)
    override val Surface = Color(0xFFF8FAFA)
    override val SurfaceBright = Color(0xFFF8FAFA)
    override val SurfaceContainer = Color(0xFFECEEEF)
    override val SurfaceContainerHigh = Color(0xFFE6E8E9)
    override val SurfaceContainerHighest = Color(0xFFE1E3E3)
    override val SurfaceContainerLow = Color(0xFFF2F4F4)
    override val SurfaceContainerLowest = Color(0xFFFFFFFF)
    override val SurfaceDim = Color(0xFFD8DADB)
    override val SurfaceTint = Color(0xFF00658F)
    override val SurfaceVariant = Color(0xFFDBE4E6)
    override val Tertiary = Color(0xFF526600)
    override val TertiaryContainer = Color(0xFFD4ED7F)
    override val TertiaryFixed = Color(0xFFD4ED7F)
    override val TertiaryFixedDim = Color(0xFFB8D166)

    override val Brown = Color(0xffe9e3d4)
    override val Purple = Color(0xffd3bfdb)
    override val Cerulean = Color(0xffaeccdc)
    override val Gray = Color(0xffefeff1)
    override val Pink = Color(0xfff6e2dd)
    override val Blue = Color(0xffd4e4ed)
    override val Red = Color(0xfffaafa8)
    override val Yellow = Color(0xfffff8b8)
    override val Green = Color(0xffe2f6d4)
    override val Teal = Color(0xffb4ddd3)
    override val Orange = Color(0xfff39f76)
}
