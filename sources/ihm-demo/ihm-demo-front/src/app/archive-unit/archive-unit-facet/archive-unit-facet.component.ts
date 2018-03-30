import { Component, OnInit, Input, Output, ChangeDetectorRef, SimpleChanges, EventEmitter } from '@angular/core';
import { forEach } from '@angular/router/src/utils/collection';
import { Preresult } from '../../common/preresult';
import { FieldDefinition } from '../../common/search/field-definition';

@Component({
  selector: 'vitam-archive-unit-facet',
  templateUrl: './archive-unit-facet.component.html',
  styleUrls: ['./archive-unit-facet.component.css']
})

export class ArchiveUnitFacetComponent implements OnInit {
  @Input() data: any;
  @Input() label: string;
  @Input() advancedMode = false;
  @Input() public searchRequest: any = {};
  @Input() public submitFunction: (service: any, emitter: EventEmitter<any>, request: any) => void;
  @Output() responseEvent: EventEmitter<any> = new EventEmitter<any>();
  facets: any[] = [];
  facetResults: any[] = [];
  facetDLResults: any[] = [];
  facetOAResults: any[] = [];
  selectedFacets: string[] = [];
  checked: boolean;
  preSearchReturn = new Preresult();
  size: number = 100;
  currentFullYear: number = new Date().getFullYear();
  rangeStartDate: number[] = [800, this.currentFullYear];
  rangeEndDate: number[] = [800, this.currentFullYear];
  rangeMax = this.currentFullYear;
  facetSDResults: string[] = [];
  facetEDResults: string[] = [];
  facetLangResults: string[] = [];
  rangeMin = 800;
  isCollapsed = true;
  mapFacetField: Map<String, String>;

  @Input() service: any;
  @Input() preSearch: (request: any, advancedMode?: boolean) => Preresult = (x) => x;

  constructor(private changeDetectorRef: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.mapFacetField = new Map()
      .set('DescriptionLevelFacet', 'DescriptionLevel')
      .set('OriginatingAgencyFacet', '#originating_agency')
      .set('StartDateFacet', 'StartDate')
      .set('endDateFacet', 'EndDate');

  }

  ngOnChanges(changes: SimpleChanges) {
    if (!!this.data && this.data.$facetResults.length > 0) {
      this.facetResults = this.data.$facetResults;
      for (const facetResult of this.facetResults) {
        switch (facetResult.name) {
          case 'DescriptionLevelFacet': {
            this.facetDLResults = facetResult.buckets;
            break;
          }
          case 'OriginatingAgencyFacet': {
            this.facetOAResults = facetResult.buckets;
            break;
          }
          case 'LanguageFacet': {
            this.facetLangResults = facetResult.buckets;
            break;
          }
          case 'StartDateFacet': {
            this.facetSDResults = facetResult.buckets;
            break;
          }
          case 'endDateFacet': {
            this.facetEDResults = facetResult.buckets;
            break;
          }
          default: {
            console.log('No match');
            break;
          }
        }
      }
    } else {
      delete this.facetResults;
      this.isCollapsed = true;
    }
  }

  searchFacets() {
    const facets = this.facets = [];
    for (const facet of this.selectedFacets) {
      let facetCriteria: any = {};
      let dateRangeCriteria: any = {};
      switch (facet) {
        case 'DescriptionLevelFacet': {
          facetCriteria.name = 'DescriptionLevelFacet';
          facetCriteria.field = 'DescriptionLevel';
          facetCriteria.size = this.size;
          facetCriteria.order = 'ASC';
          facetCriteria.facetType = 'TERMS';
          facets.push(facetCriteria);
          break;
        }
        case 'OriginatingAgencyFacet': {
          facetCriteria.name = 'OriginatingAgencyFacet';
          facetCriteria.field = '#originating_agency';
          facetCriteria.size = this.size;
          facetCriteria.order = 'ASC';
          facetCriteria.facetType = 'TERMS';
          facets.push(facetCriteria);
          break;
        }
        case 'StartDateFacet': {
          facetCriteria.name = 'StartDateFacet';
          facetCriteria.field = 'StartDate';
          facetCriteria.format = 'yyyy';
          dateRangeCriteria.dateMin = this.rangeStartDate[0];
          dateRangeCriteria.dateMax = this.rangeStartDate[1];
          facetCriteria.ranges = new Array(dateRangeCriteria);
          facetCriteria.facetType = 'DATE_RANGE';
          facets.push(facetCriteria);
          break;
        }
        case 'endDateFacet': {
          facetCriteria.name = 'endDateFacet';
          facetCriteria.field = 'EndDate';
          facetCriteria.format = 'yyyy',
            dateRangeCriteria.dateMin = this.rangeEndDate[0];
          dateRangeCriteria.dateMax = this.rangeEndDate[1];
          facetCriteria.ranges = new Array(dateRangeCriteria);
          facetCriteria.facetType = 'DATE_RANGE';
          facets.push(facetCriteria);
          break;
        }
        case 'LanguageFacet': {
          facetCriteria.name = 'LanguageFacet';
          facetCriteria.field = '#originating_agency';
          facetCriteria.size = this.size;
          facetCriteria.order = 'ASC';
          facetCriteria.facetType = 'TERMS';
          facets.push(facetCriteria);
          break;
        }
        default: {
          console.log('No matchs');
          break;
        }
      }
    }

    this.searchRequest.facets = this.facets;
    delete this.searchRequest.requestFacet;

    this.processPreSearch(this.searchRequest);
  }

  research(selectedFacetValue, facetField) {
    let facetSearchCriteria: any = {};
    facetSearchCriteria.field = this.mapFacetField.get(facetField);
    facetSearchCriteria.value = selectedFacetValue;

    this.searchRequest.requestFacet = facetSearchCriteria;
    this.searchRequest.facets = [];
    this.isCollapsed = !this.isCollapsed;

    this.processPreSearch(this.searchRequest);
  }

  processPreSearch(request: any) {
    this.preSearchReturn = this.preSearch(this.searchRequest, this.advancedMode);
    if (this.preSearchReturn.success) {
      this.submitFunction(this.service, this.responseEvent, this.preSearchReturn.request);
    } else {
      this.preSearchReturn.searchProcessError = this.preSearchReturn.searchProcessError;
    }
  }

  clearFacets() {
    delete this.facets;
    delete this.selectedFacets;
    delete this.facetDLResults;
    delete this.facetOAResults;
    delete this.facetSDResults;
    delete this.facetEDResults;
    delete this.searchRequest.requestFacet;

    this.processPreSearch(this.searchRequest);
  }
}


