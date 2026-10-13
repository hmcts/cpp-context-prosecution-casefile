package uk.gov.moj.cpp.prosecution.casefile.service;

import static jakarta.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;

import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;

public class RefDataHelperTest {

    @Test
    public void shouldMapAllRequiredFieldsFromJsonToOrganisationUnitWithCourtroomReferenceData() {
        final JsonObject json = createObjectBuilder()
                .add("id", "f8254db1-1683-483e-afb3-b87fde5a0a26")
                .add("oucode", "C55BN00")
                .add("oucodeL1Code", "C")
                .add("oucodeL1Name", "Magistrates' Courts")
                .add("oucodeL3Code", "B01KR")
                .add("oucodeL3Name", "Port Talbot")
                .add("oucodeL3WelshName", "Welsh Name")
                .add("address1", "The Court House")
                .add("address2", "Cramic Way")
                .add("address3", "Port Talbot")
                .add("address4", "West Glam.")
                .add("address5", "address line 5")
                .add("postcode", "SA13 1RU")
                .add("defaultStartTime", "10:30")
                .add("defaultDurationHrs", "10:30")
                .build();

        final OrganisationUnitWithCourtroomReferenceData result = RefDataHelper.asOrganisationUnitWithCourtroomRefData().apply(json);

        assertThat(result, is(notNullValue()));
        assertThat(result.getId(), is("f8254db1-1683-483e-afb3-b87fde5a0a26"));
        assertThat(result.getOucode(), is("C55BN00"));
        assertThat(result.getOucodeL1Code(), is("C"));
        assertThat(result.getOucodeL1Name(), is("Magistrates' Courts"));
        assertThat(result.getOucodeL3Code(), is("B01KR"));
        assertThat(result.getOucodeL3Name(), is("Port Talbot"));
        assertThat(result.getOucodeL3WelshName(), is("Welsh Name"));
        assertThat(result.getAddress1(), is("The Court House"));
        assertThat(result.getAddress2(), is("Cramic Way"));
        assertThat(result.getDefaultStartTime(), is("10:30"));
        assertThat(result.getDefaultDurationHrs(), is("10:30"));
    }

    @Test
    public void shouldMapNestedCourtRoomWhenPresent() {
        final JsonObject json = createObjectBuilder()
                .add("id", "f8254db1-1683-483e-afb3-b87fde5a0a26")
                .add("oucode", "C55BN00")
                .add("oucodeL1Code", "C")
                .add("oucodeL1Name", "Magistrates' Courts")
                .add("oucodeL3Code", "B01KR")
                .add("oucodeL3Name", "Port Talbot")
                .add("address1", "The Court House")
                .add("address2", "Cramic Way")
                .add("defaultStartTime", "10:30")
                .add("defaultDurationHrs", "10:30")
                .add("courtRoom", createObjectBuilder()
                        .add("id", "9e4932f7-97b2-3010-b942-ddd2624e4dd8")
                        .add("courtroomId", 2330)
                        .add("courtroomName", "Courtroom 01"))
                .build();

        final OrganisationUnitWithCourtroomReferenceData result = RefDataHelper.asOrganisationUnitWithCourtroomRefData().apply(json);

        assertThat(result.getCourtRoom(), is(notNullValue()));
        assertThat(result.getCourtRoom().getId(), is("9e4932f7-97b2-3010-b942-ddd2624e4dd8"));
        assertThat(result.getCourtRoom().getCourtroomId(), is(2330));
        assertThat(result.getCourtRoom().getCourtroomName(), is("Courtroom 01"));
    }

    @Test
    public void shouldLeaveCourtRoomNullWhenAbsentFromJson() {
        final JsonObject json = createObjectBuilder()
                .add("id", "f8254db1-1683-483e-afb3-b87fde5a0a26")
                .add("oucode", "C55BN00")
                .add("oucodeL1Code", "C")
                .add("oucodeL1Name", "Magistrates' Courts")
                .add("oucodeL3Code", "B01KR")
                .add("oucodeL3Name", "Port Talbot")
                .add("address1", "The Court House")
                .add("address2", "Cramic Way")
                .add("defaultStartTime", "10:30")
                .add("defaultDurationHrs", "10:30")
                .build();

        final OrganisationUnitWithCourtroomReferenceData result = RefDataHelper.asOrganisationUnitWithCourtroomRefData().apply(json);

        assertThat(result.getCourtRoom(), is(nullValue()));
    }

    @Test
    public void shouldMapIsWelshWhenPresent() {
        final JsonObject json = createObjectBuilder()
                .add("id", "abc")
                .add("oucode", "C55BN00")
                .add("oucodeL1Code", "C")
                .add("oucodeL1Name", "Magistrates' Courts")
                .add("oucodeL3Code", "B01KR")
                .add("oucodeL3Name", "Port Talbot")
                .add("address1", "The Court House")
                .add("address2", "Cramic Way")
                .add("defaultStartTime", "10:30")
                .add("defaultDurationHrs", "10:30")
                .add("isWelsh", true)
                .build();

        final OrganisationUnitWithCourtroomReferenceData result = RefDataHelper.asOrganisationUnitWithCourtroomRefData().apply(json);

        assertThat(result.getIsWelsh(), is(true));
    }

    @Test
    public void shouldLeaveIsWelshNullWhenAbsentFromJson() {
        final JsonObject json = createObjectBuilder()
                .add("id", "abc")
                .add("oucode", "C55BN00")
                .add("oucodeL1Code", "C")
                .add("oucodeL1Name", "Magistrates' Courts")
                .add("oucodeL3Code", "B01KR")
                .add("oucodeL3Name", "Port Talbot")
                .add("address1", "The Court House")
                .add("address2", "Cramic Way")
                .add("defaultStartTime", "10:30")
                .add("defaultDurationHrs", "10:30")
                .build();

        final OrganisationUnitWithCourtroomReferenceData result = RefDataHelper.asOrganisationUnitWithCourtroomRefData().apply(json);

        assertThat(result.getIsWelsh(), is(nullValue()));
    }
}
