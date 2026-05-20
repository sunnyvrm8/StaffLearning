// Scenario: legacy WMS returns opaque codes; fulfillment uses PickList status
// Demonstrates: Anti-corruption layer on WarehousePort
// Trade-off: explicit error mapping at boundary vs bool-only port

package main

type LegacyPickResponse struct {
	WhCode string
	PickID string
}

type PickStatus string

const (
	PickPending     PickStatus = "PENDING"
	PickReadyToShip PickStatus = "READY_TO_SHIP"
	PickUnknown     PickStatus = "UNKNOWN"
)

type PickListView struct {
	PickID string
	Status PickStatus
}

type WarehousePort interface {
	FetchPick(pickID string) (PickListView, error)
}

type WarehouseACL struct{ legacy LegacyWmsClient }

func (a WarehouseACL) FetchPick(pickID string) (PickListView, error) {
	raw, err := a.legacy.GetPick(pickID)
	if err != nil {
		return PickListView{}, err
	}
	status := PickUnknown
	switch raw.WhCode {
	case "LINE_OK", "WH_REQ_7":
		status = PickReadyToShip
	case "PICKING":
		status = PickPending
	}
	return PickListView{PickID: raw.PickID, Status: status}, nil
}

type LegacyWmsClient struct{}

func (LegacyWmsClient) GetPick(id string) (LegacyPickResponse, error) {
	return LegacyPickResponse{WhCode: "LINE_OK", PickID: id}, nil
}
