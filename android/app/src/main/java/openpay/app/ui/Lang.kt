package openpay.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Tiny string table. English literals are the keys, so a missing Hindi
 * entry can only fall back to English, never to a raw key. Parameterized
 * strings use {placeholders}.
 */
object Lang {
    val current = MutableStateFlow("en")
    val names: List<Pair<String, String>> = listOf("en" to "English", "hi" to "हिन्दी")

    fun set(code: String) {
        current.value = code
    }
}

private val hi: Map<String, String> = mapOf(
    // onboarding
    "Pay even with zero signal." to "सिग्नल न होने पर भी भुगतान करें।",
    "One payment, four rails" to "एक भुगतान, चार रास्ते",
    "With data, OpenPay hands off to your UPI app. With signal only, it dials *99#, or the 123Pay line when you are on Jio. With nothing at all, issue a signed Note from the ticket tab: it verifies offline and settles over UPI later." to
        "डेटा हो तो OpenPay आपका UPI ऐप खोल देता है। सिर्फ़ सिग्नल हो तो *99# डायल करता है, और Jio पर 123Pay कॉल। कुछ भी न हो तो टिकट टैब से साइन किया नोट बनाएं: वह ऑफ़लाइन जाँचा जाता है और बाद में UPI से चुकता होता है।",
    "Your PIN stays yours" to "आपका PIN आपके पास रहता है",
    "Your UPI PIN is typed on the carrier menu or the call keypad. OpenPay never sees it and never asks for it." to
        "आपका UPI PIN कैरियर मेन्यू या कॉल कीपैड पर टाइप होता है। OpenPay उसे कभी नहीं देखता, न माँगता है।",
    "No internet, actually" to "इंटरनेट नहीं, सच में",
    "The app ships without the internet permission. It cannot reach any server. Every record is encrypted on this phone." to
        "ऐप में internet permission है ही नहीं। वह किसी सर्वर से बात नहीं कर सकता। हर रिकॉर्ड इसी फ़ोन पर एन्क्रिप्टेड रहता है।",
    "Pick your bank" to "अपना बैंक चुनें",
    "Confirmation SMS are only trusted from your bank's sender ID." to "पुष्टि का SMS सिर्फ़ आपके बैंक के sender ID से मान्य है।",
    "Allow calling and SMS" to "कॉल और SMS की अनुमति दें",
    "Start using OpenPay" to "OpenPay शुरू करें",
    "OpenPay is not a bank. Payments settle on UPI rails through your bank. *99# and UPI 123Pay are NPCI services." to
        "OpenPay बैंक नहीं है। भुगतान आपके बैंक की UPI से होते हैं। *99# और UPI 123Pay, NPCI की सेवाएँ हैं।",
    // pay screen
    "Payee name" to "प्राप्तकर्ता का नाम",
    "UPI ID, like name@bank" to "UPI ID, जैसे name@bank",
    "Or payee phone (for 123Pay)" to "या प्राप्तकर्ता का फ़ोन (123Pay के लिए)",
    "Amount in rupees" to "रुपये में रक़म",
    "Note, optional" to "टिप्पणी, वैकल्पिक",
    "Pay" to "भुगतान करें",
    "Pay ₹{amt}" to "₹{amt} भुगतान करें",
    "Net: {a} · Carrier: {b}" to "नेट: {a} · कैरियर: {b}",
    "online" to "ऑनलाइन",
    "signal only" to "सिर्फ़ सिग्नल",
    "none" to "नहीं",
    "unknown" to "अज्ञात",
    "Best rail right now" to "अभी का सही रास्ता",
    "Data available. Handed to your UPI app." to "डेटा है। आपके UPI ऐप को सौंप दिया जाएगा।",
    "Signal only. Carrier menu, no data needed." to "सिर्फ़ सिग्नल। कैरियर मेन्यू, डेटा ज़रूरी नहीं।",
    "Signal only. Works on Jio." to "सिर्फ़ सिग्नल। Jio पर चलता है।",
    "Needs a valid 10 digit phone number." to "मान्य 10 अंकों का फ़ोन नंबर चाहिए।",
    "Add a UPI ID or a phone number first." to "पहले UPI ID या फ़ोन नंबर जोड़ें।",
    "Jio carries no USSD and the 123Pay call needs the payee's phone number. Add it, or wait for data." to
        "Jio पर USSD नहीं चलता और 123Pay कॉल को प्राप्तकर्ता का फ़ोन चाहिए। जोड़ें, या डेटा का इंतज़ार करें।",
    "No network at all right now. Create an OpenPay Note instead: it verifies offline and settles over UPI later. The ticket icon opens Notes." to
        "अभी नेट है ही नहीं। इसकी जगह OpenPay नोट बनाएं: वह ऑफ़लाइन जाँचा जाता है और बाद में UPI से चुकता होता है। टिकट आइकन से नोट खुलता है।",
    "No SIM detected on this phone." to "इस फ़ोन में SIM नहीं मिला।",
    "The 123Pay call needs whole rupees. Drop the paise or use a UPI ID with data." to
        "123Pay कॉल में पूरे रुपये चाहिए। पैसे हटाएँ या डेटा पर UPI ID इस्तेमाल करें।",
    "Your UPI PIN is typed on the carrier menu or the call keypad. OpenPay never sees it." to
        "आपका UPI PIN कैरियर मेन्यू या कॉल कीपैड पर टाइप होता है। OpenPay उसे कभी नहीं देखता।",
    "Grant call and SMS, then tap Pay again" to "कॉल और SMS की अनुमति दें, फिर भुगतान दबाएँ",
    "No UPI app installed" to "कोई UPI ऐप नहीं है",
    "The dialer refused the call" to "डायलर ने कॉल से इनकार किया",
    "Not a UPI QR code" to "यह UPI QR नहीं है",
    "Finish it on the USSD menu" to "USSD मेन्यू पर पूरा करें",
    "Finish it on the call" to "कॉल पर पूरा करें",
    "Type the UPI ID when asked: {vpa}" to "पूछे जाने पर UPI ID टाइप करें: {vpa}",
    "Type the amount: Rs {amt}" to "रक़म टाइप करें: ₹{amt}",
    "Type a remark if the menu asks, or send 1 to skip" to "मेन्यू माँगे तो टिप्पणी लिखें, नहीं तो 1 भेजकर छोड़ दें",
    "Type your UPI PIN. It stays on the carrier menu, OpenPay never sees it" to
        "अपना UPI PIN टाइप करें। वह कैरियर मेन्यू पर ही रहता है, OpenPay नहीं देखता",
    "Confirm. Your bank's SMS will close this payment here automatically" to
        "पुष्टि करें। बैंक का SMS इस भुगतान को यहीं अपने आप पूरा कर देगा",
    "The call starts itself and keys in the menu, recipient and Rs {amt}" to
        "कॉल अपने आप शुरू होकर मेन्यू, प्राप्तकर्ता और ₹{amt} दबा देती है",
    "Listen for the UPI PIN prompt and dial your PIN on the call keypad" to
        "UPI PIN के निर्देश पर कॉल कीपैड से PIN डायल करें",
    "Hang on the line until the IVR reads out the reference number" to
        "IVR जब तक संदर्भ नंबर न पढ़े, लाइन पर बने रहें",
    "Your bank's SMS will close this payment here automatically" to "बैंक का SMS इस भुगतान को यहीं अपने आप पूरा कर देगा",
    "Recorded as pending. Your bank's SMS closes it automatically, usually within a minute." to
        "पेंडिंग दर्ज हुआ। बैंक का SMS इसे अपने आप पुष्ट कर देता है, आमतौर पर एक मिनट में।",
    // history
    "History" to "इतिहास",
    "Nothing yet. Payments appear here the moment they start." to "अभी कुछ नहीं। भुगतान शुरू होते ही यहाँ दिखेंगे।",
    // detail
    "Payment" to "भुगतान",
    "When" to "कब",
    "Phone" to "फ़ोन",
    "Note" to "टिप्पणी",
    "Rail" to "रास्ता",
    "Bank ref" to "बैंक संदर्भ",
    "Confirmed by" to "पुष्टि",
    "your bank's SMS" to "आपके बैंक के SMS से",
    "the UPI app's response" to "UPI ऐप के जवाब से",
    "nothing yet" to "अभी कुछ नहीं",
    "Record" to "रिकॉर्ड",
    "Confirmed. Bank reference {r}." to "पुष्ट हुई। बैंक संदर्भ {r}।",
    "Confirmed by the UPI app response." to "UPI ऐप के जवाब से पुष्ट हुई।",
    "Pending past the SMS window." to "SMS समय सीमा निकल गई, फिर भी पेंडिंग।",
    "If the money left your account, banks must auto reverse failed payments by T+1, and refunds must land within T+5 working days. After that the bank owes Rs 100 per day (RBI turn around time rules)." to
        "अगर पैसे खाते से गए हैं तो बैंक असफल भुगतान T+1 तक अपने आप वापस करता है, और रिफ़ंड T+5 कार्यदिवसों में आना चाहिए। उसके बाद बैंक ₹100 प्रति दिन देता है (RBI नियम)।",
    "Waiting for your bank's SMS. Usually under a minute." to "बैंक के SMS का इंतज़ार। आमतौर पर एक मिनट से कम।",
    "Marked failed. Nothing left this phone." to "असफल दर्ज। इस फ़ोन से कुछ नहीं गया।",
    "Open NPCI help" to "NPCI मदद खोलें",
    "Copy escalation email" to "शिकायत ईमेल कॉपी करें",
    "Escalation email copied" to "शिकायत ईमेल कॉपी हुआ",
    "Share record" to "रिकॉर्ड साझा करें",
    "Share payment record" to "भुगतान रिकॉर्ड साझा करें",
    "This payment never happened" to "यह भुगतान कभी हुआ ही नहीं",
    "Mark as never paid?" to "कभी हुआ नहीं दर्ज करें?",
    "This only changes your local record. It cannot reverse a real debit." to
        "इससे सिर्फ़ स्थानीय रिकॉर्ड बदलता है। असली डेबिट वापस नहीं होता।",
    "Mark failed" to "असफल दर्ज करें",
    "Keep pending" to "पेंडिंग रहने दें",
    "This payment is gone from the ledger." to "यह भुगतान बही खाते से हट गया।",
    // settings
    "Settings" to "सेटिंग्स",
    "Bank SMS matching" to "बैंक SMS मिलान",
    "Other bank: sender matching is off, only amount plus debit wording counts." to
        "दूसरा बैंक: sender मिलान बंद है, सिर्फ़ रक़म और डेबिट शब्द गिने जाते हैं।",
    "Only SMS from {s} close a payment." to "सिर्फ़ {s} के SMS भुगतान पूरा करते हैं।",
    "This phone" to "यह फ़ोन",
    "Carrier" to "कैरियर",
    "Network" to "नेट",
    "Jio detected" to "Jio मिला",
    "yes, IVR rail" to "हाँ, IVR रास्ता",
    "no, USSD rail first" to "नहीं, पहले USSD रास्ता",
    "Trust" to "भरोसा",
    "absent, by design" to "नहीं है, जानबूझकर",
    "Accounts" to "खाते",
    "none" to "कोई नहीं",
    "Analytics" to "एनालिटिक्स",
    "Ledger" to "बही खाता",
    "encrypted on device" to "फ़ोन पर एन्क्रिप्टेड",
    "Version" to "संस्करण",
    "Source code" to "सोर्स कोड",
    "Share ledger" to "बही खाता साझा करें",
    "No payments yet." to "अभी कोई भुगतान नहीं।",
    "Your bank" to "आपका बैंक",
    "Done" to "हो गया",
    "Language" to "भाषा",
    "Accessibility" to "पहुँच",
    "Elder mode, large text" to "बड़ा टेक्स्ट मोड",
    "Export CSV" to "CSV निर्यात करें",
    "Ledger exported" to "बही खाता निर्यात हुआ",
    // notes
    "Notes · issue" to "नोट · बनाएँ",
    "Notes · verify" to "नोट · जाँच",
    "Issue" to "बनाएँ",
    "Verify" to "जाँच",
    "A Note is a signed promise. The payee verifies it offline, it settles over UPI later." to
        "नोट एक साइन किया वादा है। प्राप्तकर्ता उसे ऑफ़लाइन जाँचता है, बाद में UPI से चुकता होता है।",
    "Rs {a} of Rs 10,000 open" to "₹10,000 में से ₹{a} खुले हैं",
    "Your name, shown to the payee" to "आपका नाम, प्राप्तकर्ता को दिखेगा",
    "Payee name" to "प्राप्तकर्ता का नाम",
    "Payee UPI ID, for settling" to "प्राप्तकर्ता की UPI ID, चुकता करने के लिए",
    "Or payee phone" to "या प्राप्तकर्ता का फ़ोन",
    "Amount in rupees, max 2000" to "रुपये में रक़म, अधिकतम 2000",
    "Memo, optional" to "विवरण, वैकल्पिक",
    "30 min" to "30 मिनट",
    "1 hour" to "1 घंटा",
    "6 hours" to "6 घंटे",
    "24 hours" to "24 घंटे",
    "Create Note" to "नोट बनाएँ",
    "Note {serial}" to "नोट {serial}",
    "The payee scans this in Verify mode. No signal needed on either phone." to
        "प्राप्तकर्ता इसे जाँच मोड में स्कैन करता है। दोनों फ़ोनों को सिग्नल ज़रूरी नहीं।",
    "Note QR" to "नोट QR",
    "Settle over UPI" to "UPI से चुकता करें",
    "Mark settled" to "चुकता दर्ज करें",
    "Note settled" to "नोट चुकता हुआ",
    "Settle all open notes" to "सारे खुले नोट चुकता करें",
    "Copy note text" to "नोट टेक्स्ट कॉपी करें",
    "Share note" to "नोट साझा करें",
    "Note text copied" to "नोट टेक्स्ट कॉपी हुआ",
    "Merchant mode. Scan a customer's Note with zero signal: the signature, expiry, cap and serial are all checked on this phone." to
        "दुकान मोड। बिना सिग्नल ग्राहक का नोट स्कैन करें: सिग्नेचर, समय सीमा, सीमा और सीरियल सब इसी फ़ोन पर जाँचे जाते हैं।",
    "Scan a Note" to "नोट स्कैन करें",
    "Scan an OpenPay Note" to "OpenPay नोट स्कैन करें",
    "Duplicate serial, already seen." to "सीरियल दोहराया गया, पहले ही देखा।",
    "Verified. Signature checks out." to "जाँच पास। सिग्नेचर सही है।",
    "Known payer." to "जाना पहचाना प्राप्तकर्ता।",
    "New payer key. Save it to trust this key next time." to "नई कुंजी। सहेजें तो अगली बार भरोसा रहेगा।",
    "Save this payer" to "यह प्राप्तकर्ता सहेजें",
    "Payer saved" to "प्राप्तकर्ता सहेजा गया",
    "Rejected: {r}" to "अस्वीकृत: {r}",
    "Note verified. Rupees {a} from {f}." to "नोट पुष्ट। {f} से ₹{a}।",
    "Note rejected." to "नोट अस्वीकृत।",
    "known payer" to "जाना पहचाना",
    "new key" to "नई कुंजी",
    "Payments" to "भुगतान",
    "Spending" to "खर्च",
    "No spending captured yet. Your bank's debit SMS become a private log here, on this phone only." to
        "अभी कोई खर्च नहीं। बैंक के डेबिट SMS यहाँ एक निजी बही बनते हैं, सिर्फ़ इसी फ़ोन पर।",
    "Rs {a}" to "₹{a}",
    "Reads your bank's debit SMS on device. Nothing leaves this phone." to
        "बैंक के डेबिट SMS फ़ोन पर ही पढ़े जाते हैं। इस फ़ोन से कुछ बाहर नहीं जाता।",
    "Expense log" to "खर्च बही",
    "Private spending log" to "निजी खर्च बही",
    "Reads bank debit SMS on device to build a private spending log. No internet, ever." to
        "खर्च बही के लिए बैंक के डेबिट SMS फ़ोन पर ही पढ़े जाते हैं। इंटरनेट कभी नहीं।",
    "Split" to "बाँटें",
    "Notes · split" to "नोट · बाँटें",
    "One bill, many notes. OpenPay cuts the total into fair shares and signs a Note per person, for the same UPI ID." to
        "एक बिल, कई नोट। OpenPay कुल रक़म को बराबर हिस्सों में बाँटकर हर हिस्से का एक नोट बनाता है, एक ही UPI ID के लिए।",
    "Total amount in rupees" to "कुल रक़म (रुपये)",
    "Number of shares" to "कितने हिस्से",
    "Payee UPI ID, same for every note" to "प्राप्तकर्ता की UPI ID, हर नोट के लिए एक ही",
    "Names, comma separated, optional" to "नाम, अल्पविराम से, वैकल्पिक",
    "Each share" to "हर हिस्सा",
    "Person {n}" to "व्यक्ति {n}",
    "Split {i}/{n}" to "हिस्सा {i}/{n}",
    "Create {n} notes" to "{n} नोट बनाएँ",
    "Total must be at least the number of shares" to "कुल रक़म हिस्सों की संख्या से कम नहीं हो सकती",
    "Show QR" to "QR दिखाएँ",
    "Pay again" to "दोबारा भुगतान करें",
    "Possible double payment" to "दो बार भुगतान हो सकता है",
    "You started a payment of Rs {a} to {p} {s} seconds ago and it has not confirmed yet. Retrying can double debit. Wait for the SMS or continue." to
        "आपने {p} को ₹{a} {s} सेकंड पहले भेजा था और वह अभी पुष्ट नहीं हुई। दोबारा भेजने पर दो बार कट सकता है। SMS का इंतज़ार करें या आगे बढ़ें।",
    "Wait" to "रुकें",
    "Pay anyway" to "फिर भी भेजें",
    "Check balance (*99*3#)" to "बैलेंस देखें (*99*3#)",
    "Open Notes" to "नोट खोलें",
    "You have {n} open notes, the oldest is {h} hours old. Notes are promises: settle them or let them expire." to
        "आपके {n} नोट खुले हैं, सबसे पुराना {h} घंटे का। नोट वादे हैं: चुकता करें या समाप्त होने दें।",
    "New payer, big note" to "नया प्राप्तकर्ता, बड़ा नोट",
    "New payer, big note." to "नया प्राप्तकर्ता, बड़ा नोट।",
    "First note from this key and above Rs 500. Notes are promises, not money. Accept?" to
        "इस कुंजी का पहला नोट है और ₹500 से बड़ा है। नोट वादे हैं, पैसे नहीं। स्वीकार करें?",
    "Accept" to "स्वीकार",
    "Blacklisted payer. Refuse." to "प्रतिबंधित प्राप्तकर्ता। मना करें।",
    "Block this payer" to "इस प्राप्तकर्ता को रोकें",
    "Remove block" to "रोक हटाएँ",
    "Decline" to "अस्वीकार",
    "declined by you" to "आपने अस्वीकार किया",
    "MDR structuring warning" to
        "₹2,000 से बड़े P2M भुगतान पर MDR प्राप्तकर्ता व्यापारी देता है, और NPCI एक ही खरीद को छोटे भुगतानों में बाँटकर बचाने से रोकता है। इससे आपका कुछ नहीं बचता और आयकर कुल रक़म देखता है, लेन-देन का आकार नहीं। सिर्फ़ सचमुच अलग हिस्सों के लिए बाँटें।"
)

/** Composable lookup, falls back to the English literal. */
@Composable
fun s(text: String, vararg args: Pair<String, String>): String {
    val lang by Lang.current.collectAsState()
    var out = if (lang == "hi") hi[text] ?: text else text
    for ((k, v) in args) out = out.replace("{$k}", v)
    return out
}

/** Non composable lookup for callbacks and TTS. */
fun ts(text: String, vararg args: Pair<String, String>): String {
    var out = if (Lang.current.value == "hi") hi[text] ?: text else text
    for ((k, v) in args) out = out.replace("{$k}", v)
    return out
}
