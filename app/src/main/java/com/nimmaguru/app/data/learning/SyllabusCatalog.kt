package com.nimmaguru.app.data.learning

import com.nimmaguru.app.domain.model.CurriculumType
import com.nimmaguru.app.domain.model.SyllabusTopic

object SyllabusCatalog {
    val topics = listOf(
        SyllabusTopic("CBSE", "Class 7", "Mathematics", "Integers", listOf("Properties", "Operations", "Word problems")),
        SyllabusTopic("CBSE", "Class 7", "Mathematics", "Lines and Angles", listOf("Related angles", "Pairs of lines", "Transversal")),
        SyllabusTopic("CBSE", "Class 8", "Mathematics", "Linear Equations", listOf("One variable equations", "Applications")),
        SyllabusTopic("CBSE", "Class 8", "Mathematics", "Understanding Quadrilaterals", listOf("Polygons", "Parallelograms", "Special quadrilaterals")),
        SyllabusTopic("CBSE", "Class 9", "Mathematics", "Circles", listOf("Chords", "Angles", "Cyclic quadrilaterals")),
        SyllabusTopic("CBSE", "Class 9", "Mathematics", "Constructions", listOf("Bisectors", "Triangles", "Angle constructions")),
        SyllabusTopic("CBSE", "Class 10", "Mathematics", "Real Numbers", listOf("Euclid division lemma", "Fundamental theorem of arithmetic", "Decimal expansions")),
        SyllabusTopic("CBSE", "Class 10", "Mathematics", "Polynomials", listOf("Zeros of a polynomial", "Relationship between zeros and coefficients")),
        SyllabusTopic("CBSE", "Class 10", "Mathematics", "Pair of Linear Equations in Two Variables", listOf("Graphical method", "Substitution", "Elimination", "Cross multiplication")),
        SyllabusTopic("CBSE", "Class 10", "Mathematics", "Quadratic Equations", listOf("Standard form", "Factorisation", "Quadratic formula", "Discriminant")),
        SyllabusTopic("CBSE", "Class 10", "Mathematics", "Arithmetic Progressions", listOf("Nth term", "Sum of first n terms")),
        SyllabusTopic("CBSE", "Class 10", "Science", "Chemical Reactions and Equations", listOf("Balancing equations", "Types of reactions", "Oxidation and reduction")),
        SyllabusTopic("CBSE", "Class 10", "Science", "Life Processes", listOf("Nutrition", "Respiration", "Transportation", "Excretion")),
        SyllabusTopic("CBSE", "Class 10", "Science", "Electricity", listOf("Ohm law", "Resistance", "Series and parallel circuits", "Electric power")),
        SyllabusTopic("CBSE", "Class 9", "Mathematics", "Number Systems", listOf("Real numbers", "Irrational numbers", "Laws of exponents")),
        SyllabusTopic("CBSE", "Class 9", "Science", "Matter in Our Surroundings", listOf("States of matter", "Evaporation", "Change of state")),
        SyllabusTopic("Karnataka State Board", "Class 7", "Mathematics", "Simple Equations", listOf("Solving equations", "Applications")),
        SyllabusTopic("Karnataka State Board", "Class 7", "Mathematics", "Triangles", listOf("Types", "Angle sum property", "Congruence basics")),
        SyllabusTopic("Karnataka State Board", "Class 8", "Mathematics", "Squares and Square Roots", listOf("Perfect squares", "Square roots", "Applications")),
        SyllabusTopic("Karnataka State Board", "Class 8", "Mathematics", "Circles", listOf("Radius", "Diameter", "Circumference", "Area")),
        SyllabusTopic("Karnataka State Board", "Class 9", "Mathematics", "Constructions", listOf("Perpendicular bisectors", "Angle bisectors", "Triangle construction")),
        SyllabusTopic("Karnataka State Board", "Class 10", "Mathematics", "Arithmetic Progressions", listOf("Sequence patterns", "Nth term", "Sum of terms")),
        SyllabusTopic("Karnataka State Board", "Class 10", "Mathematics", "Triangles", listOf("Similarity", "Pythagoras theorem", "Applications")),
        SyllabusTopic("Karnataka State Board", "Class 10", "Mathematics", "Statistics", listOf("Mean", "Median", "Mode", "Ogive")),
        SyllabusTopic("Karnataka State Board", "Class 10", "Science", "Chemical Reactions and Equations", listOf("Chemical equations", "Types of chemical reactions", "Oxidation and reduction")),
        SyllabusTopic("Karnataka State Board", "Class 10", "Science", "Life Processes", listOf("Nutrition", "Respiration", "Transportation", "Excretion")),
        SyllabusTopic("Karnataka State Board", "Class 10", "Science", "Light Reflection and Refraction", listOf("Mirrors", "Lenses", "Image formation", "Lens formula")),
        SyllabusTopic("Karnataka State Board", "Class 9", "Mathematics", "Polynomials", listOf("Algebraic identities", "Remainder theorem", "Factorisation")),
        SyllabusTopic("Karnataka State Board", "Class 9", "Science", "Atoms and Molecules", listOf("Laws of chemical combination", "Molecules", "Mole concept"))
    )

    fun filtered(curriculumType: CurriculumType?): List<SyllabusTopic> {
        return when (curriculumType) {
            CurriculumType.CBSE -> topics.filter { it.board == "CBSE" }
            CurriculumType.KARNATAKA_STATE_BOARD -> topics.filter { it.board == "Karnataka State Board" }
            else -> topics
        }
    }

    fun formatted(curriculumType: CurriculumType? = null): String {
        return formatted(filtered(curriculumType))
    }

    fun formatted(topics: List<SyllabusTopic>): String {
        return topics.groupBy { "${it.board} • ${it.classLevel} • ${it.subject}" }
            .entries.joinToString("\n\n") { (heading, chapters) ->
                val body = chapters.joinToString("\n") { item ->
                    "- ${item.chapter}: ${item.topics.joinToString(", ")}"
                }
                "$heading\n$body"
            }
    }
}
