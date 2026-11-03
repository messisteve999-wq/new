@Composable
fun AutoContactsScreen(
    contacts: List<Pair<String, String>>,
    initial: Set<String>,
    onSave: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }
    val filtered = contacts.filter {
        it.first.contains(query, ignoreCase = true) || it.second.contains(query)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Contacts du mode automatique", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Rechercher") }, modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.padding(vertical = 8.dp)) {
            TextButton(onClick = {
                                selected = contacts.map { it.first.trim() }.toSet()
            }) { Text("Tout sélectionner") }
            TextButton(onClick = { selected = emptySet() }) { Text("Tout désélectionner") }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(filtered) { (name, number) ->
                val key = name.trim()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = key in selected,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + key else selected - key
                        }
                    )
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(name)
                        Text(number, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Button(onClick = { onSave(selected) }, modifier = Modifier.fillMaxWidth()) {
            Text("Enregistrer")
        }
    }
}
