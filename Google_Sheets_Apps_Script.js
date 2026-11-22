function doPost(e) {
  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var data = JSON.parse(e.postData.contents);

    if (data.action === "reset") {
      sheet.getRange("B5:I30").clearContent();
      sheet.getRange("C31:I31").clearContent();
      return ContentService.createTextOutput(JSON.stringify({ status: "reset_ok" }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    var dayNum = parseInt(data.day, 10);
    if (!dayNum || dayNum < 1 || dayNum > 26) {
      return ContentService.createTextOutput(JSON.stringify({ status: "invalid_day" }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    var row = 4 + dayNum;
    var km = Number(data.km) || 0;
    var rides = Number(data.rides) || 0;
    var gross = Number(data.gross) || 0;
    var petrol = Number(data.petrol) || 0;
    var fee = Number(data.fee) || 0;
    var net = gross - petrol - fee;

    sheet.getRange(row, 2, 1, 7).setValues([[data.date, km, rides, gross, petrol, fee, net]]);

    var running = 0;
    var sumKm = 0, sumRides = 0, sumGross = 0, sumPetrol = 0, sumFee = 0, sumNet = 0;
    var values = sheet.getRange("B5:H30").getValues();

    for (var i = 0; i < 26; i++) {
      var rDate = values[i][0];
      var rKm = Number(values[i][1]) || 0;
      var rRides = Number(values[i][2]) || 0;
      var rGross = Number(values[i][3]) || 0;
      var rPetrol = Number(values[i][4]) || 0;
      var rFee = Number(values[i][5]) || 0;
      var rNet = Number(values[i][6]) || 0;

      if (rDate !== "" || rKm > 0 || rRides > 0 || rGross > 0 || rPetrol > 0) {
        running += rNet;
        sumKm += rKm;
        sumRides += rRides;
        sumGross += rGross;
        sumPetrol += rPetrol;
        sumFee += rFee;
        sumNet += rNet;
        sheet.getRange(5 + i, 9).setValue(running);
      } else {
        sheet.getRange(5 + i, 9).clearContent();
      }
    }

    sheet.getRange("C31:I31").setValues([[sumKm, sumRides, sumGross, sumPetrol, sumFee, sumNet, running]]);

    return ContentService.createTextOutput(JSON.stringify({ status: "success", day: dayNum, runningTotal: running }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var rows = sheet.getRange("A5:H30").getValues();
  var entries = {};

  for (var i = 0; i < rows.length; i++) {
    var dateVal = rows[i][1];
    var km = Number(rows[i][2]) || 0;
    var rides = Number(rows[i][3]) || 0;
    var gross = Number(rows[i][4]) || 0;
    var petrol = Number(rows[i][5]) || 0;
    var fee = rows[i][6] === "" ? 40 : Number(rows[i][6]);

    if (dateVal !== "" || km > 0 || rides > 0 || gross > 0 || petrol > 0) {
      var formattedDate = "";
      if (dateVal instanceof Date) {
        formattedDate = Utilities.formatDate(dateVal, Session.getScriptTimeZone(), "yyyy-MM-dd");
      } else {
        formattedDate = String(dateVal);
      }
      entries[i + 1] = {
        date: formattedDate,
        km: km,
        rides: rides,
        gross: gross,
        petrol: petrol,
        fee: fee
      };
    }
  }

  return ContentService.createTextOutput(JSON.stringify({ status: "ok", entries: entries }))
    .setMimeType(ContentService.MimeType.JSON);
}
