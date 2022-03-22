package cz.cleevio.core.utils

import java.text.Normalizer

fun String.stripAccents(): String {
	var normalize = Normalizer.normalize(this.toLowerCase(), Normalizer.Form.NFD)
	normalize = normalize.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
	return normalize
}

fun String.toDoubleOrNullable(): Double? =
	this.replace(',', '.').toDoubleOrNull()

fun String.toValidPhoneNumber(): String {
	return this
		.replace("\\s".toRegex(), "")
		.replace("-".toRegex(), "")
}

fun String.isPhoneValid(): Boolean =
	//this.matches("^\\+(?:[0-9] ?){6,14}[0-9]\$".toRegex())	//todo: discuss + sign for prefix
	this.matches("^(?:[0-9] ?){6,14}[0-9]\$".toRegex())