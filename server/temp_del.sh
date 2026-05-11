# Use the token from login response
TOKEN="7ee3dc72-ae3e-4ff0-8d4a-f558c8f8c98e"

# Send several strokes with different patterns
for i in {1..10}; do
  curl -X POST http://localhost:3343/raw_stroke \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"stroke\":[
        {\"time_ms\":$((1000 + $i * 100)),\"action\":0,\"x\":$((100 + $i * 10)),\"y\":$((200 + $i * 5)),\"pressure\":0.3,\"area\":0.05,\"finger_orientation\":0.0},
        {\"time_ms\":$((1010 + $i * 100)),\"action\":2,\"x\":$((110 + $i * 10)),\"y\":$((210 + $i * 5)),\"pressure\":0.32,\"area\":0.05,\"finger_orientation\":0.0},
        {\"time_ms\":$((1030 + $i * 100)),\"action\":1,\"x\":$((130 + $i * 10)),\"y\":$((230 + $i * 5)),\"pressure\":0.4,\"area\":0.06,\"finger_orientation\":0.0}
      ],
      \"dpi_x\":420, \"dpi_y\":420, \"phone_orientation\":1, \"phone_id\":1
    }"
  echo ""
done
