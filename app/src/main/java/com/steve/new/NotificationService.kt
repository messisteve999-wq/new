if (autoModeOn && AutoContactsStore.isAllowed(context, sender)) {
    sendAutoReply(context, sender, message)
} else {
    showSuggestionOnly(context, sender, message)
}
