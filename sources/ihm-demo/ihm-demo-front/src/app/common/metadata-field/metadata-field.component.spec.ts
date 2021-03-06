import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { MetadataFieldComponent } from './metadata-field.component';
import { ArchiveUnitHelper } from "../../archive-unit/archive-unit.helper";
import { ReferentialHelper } from "../../referentials/referential.helper";

describe('MetadataFieldComponent', () => {
  let component: MetadataFieldComponent;
  let fixture: ComponentFixture<MetadataFieldComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [ ArchiveUnitHelper, ReferentialHelper ],
      declarations: [ MetadataFieldComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MetadataFieldComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it ('should display error message when date is invalid', (done) => {
    component.dateValue = null;
    component.checkDateValid();
    setTimeout(() => {
      expect(component.displayError).toBeTruthy();
      done();
    }, 250);
  });

  it ('should not display error message when date is valid', (done) => {
    component.dateValue = new Date();
      component.checkDateValid();
    setTimeout(() => {
      expect(component.displayError).toBeFalsy();
      done();
    }, 250);
  });
});
