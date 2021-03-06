import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {ArchiveUnitHelper} from '../../archive-unit.helper';
import {ArchiveUnitService} from '../../archive-unit.service';
import {ConfirmationService} from 'primeng/primeng';
import {DateService} from '../../../common/utils/date.service';

@Component({
  selector: 'vitam-archive-rule-bloc',
  templateUrl: './archive-rule-bloc.component.html',
  styleUrls: ['./archive-rule-bloc.component.css']
})
export class ArchiveRuleBlocComponent implements OnInit, OnChanges {
  @Input() inheritedRules;
  @Input() management;
  @Input() id;
  public rulesCategories = this.archiveUnitHelper.rulesCategories;
  public finalActions = {
    StorageRule: this.archiveUnitHelper.storageFinalAction,
    AppraisalRule: this.archiveUnitHelper.appraisalFinalAction
  };
  public titles = {};
  public displayDetails = {};
  public update = false;
  public updatedFields: any = {};
  public saveOriginal = '';
  public saveRunning = false;
  public displayOK = false;
  public displayKO = false;
  public errorMessage = '';
  public messageToDisplay: string;

  frLocale = {
    dayNames: ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'],
    dayNamesShort: ['Dim.', 'Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.'],
    dayNamesMin: ['Di', 'Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa'],
    monthNames: ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Aout', 'Septembre', 'Octobre', 'Novembre', 'Décembre'],
    monthNamesShort: ['Jan', 'Fév', 'Mars', 'Avr', 'Mai', 'Juin', 'Juil', 'Aou', 'Sep', 'Oct', 'Nov', 'Dec'],
    firstDayOfWeek: 1,
    today: 'Aujourd\'hui',
    clear: 'Vider'
  };

  constructor(public archiveUnitHelper: ArchiveUnitHelper, private archiveUnitService: ArchiveUnitService,
              public confirmationService: ConfirmationService) {

  }

  ngOnChanges() {
    this.saveOriginal = JSON.stringify(this.management);
    this.displayDetails = {};
  }

  ngOnInit() {
    this.saveOriginal = JSON.stringify(this.management);
  }

  isInherited(id) {
    return id === this.id ? 'Non' : 'Oui';
  }

  toogleDetails(category, rule, id) {
    this.displayDetails[rule + '.' + id] = !this.displayDetails[rule + '.' + id];
    if (this.inheritedRules[category][rule][id].path.length === 1 && (!this.titles[rule + '.' + id])) {
      this.archiveUnitService.getDetails(id)
        .subscribe(response => {
          this.titles[rule + '.' + id] = response.$results[0].Title;
        });
    }
  }

  switchUpdateMode() {
    this.update = !this.update;
    if (!this.update) {
      this.management = JSON.parse(this.saveOriginal);
      this.updatedFields = {};
    } else {
      this.initUpdatedRules();
    }
  }

  getMgtRule(category, id) {
    if (!this.management[category]) {
      return null;
    }
    for (const rule of this.management[category].Rules) {
      if (rule.Rule === id) {
        return rule;
      }
    }
    return null;
  }

  checkUpdate(category, rule): boolean {
    // FIXME Errors with StartDate ?
    const mgtRule = this.getMgtRule(category, rule.oldId);
    if (!this.management[category] || !mgtRule) {
      return false;
    }
    return rule.Rule !== mgtRule.Rule ||
      rule.FinalAction !== mgtRule.FinalAction ||
      rule.StartDate !== mgtRule.StartDate
  }

  checkFinalActionUpdated(category): boolean {
    if (!this.management || !this.management[category]) {
      return !!this.updatedFields[category].FinalAction;
    }

    return this.updatedFields[category].FinalAction !== this.management[category].FinalAction;
  }
    
  checkClassificationLevelUpdated(category): boolean {
    if (!this.management || !this.management[category]) {
      return !!this.updatedFields[category].ClassificationLevel;
    }

    return this.updatedFields[category].ClassificationLevel !== this.management[category].ClassificationLevel;
  }
    
  checkClassificationOwnerUpdated(category): boolean {
    if (!this.management || !this.management[category]) {
      return !!this.updatedFields[category].ClassificationOwner;
    }

    return this.updatedFields[category].ClassificationOwner !== this.management[category].ClassificationOwner;
  }

  getUpdatedRules() {
    // ruleCategory ~= this.updatedFields
    // updatedRules > the array that must be updated and pushed in request.
    // updatedRules = [{'CategName': {'Rules': ..., 'Inheritance': ...}, {...}, ...];

    const updateInfo = {
      updated: 0,
      added: 0,
      deleted: 0,
      categories: [],
      rules: []
    };


    for (const category in this.updatedFields) {
      if (!this.updatedFields.hasOwnProperty(category)) { continue; }

      // categoryName = need looping over categories in this function and push in updatedRules[]
      let isCategoryUpdated = false;
      const newCategory: any = {
        Rules: [],
        /*FinalAction: '',
        Inheritance: {}*/
      };

      for (let i = 0, len = this.updatedFields[category].Rules.length; i < len; i++) {
        const rule = this.updatedFields[category].Rules[i];
        rule.StartDate = DateService.handleDateForRules(rule.StartDate);
        if (!rule.inherited) {
          if (!rule.StartDate) { delete rule.StartDate; }
          if (rule.newRule) {
            // New Rule
            isCategoryUpdated = true;
            const addedRule = JSON.parse(JSON.stringify(rule));
            delete addedRule.newRule;
            newCategory.Rules.push(addedRule);

            updateInfo.added++;
          } else if (rule.oldRule) {
            // Deleted rule
            isCategoryUpdated = true;
            updateInfo.deleted++;
          } else if (this.checkUpdate(category, rule)) {
            // Updated rule
            isCategoryUpdated = true;
            const updatedRule = JSON.parse(JSON.stringify(rule));
            delete updatedRule.oldId;
            delete updatedRule.EndDate;
            newCategory.Rules.push(updatedRule);
            updateInfo.updated++;
          } else {
            // Non-Updated Old Rule
            const updatedRule = JSON.parse(JSON.stringify(rule));
            delete updatedRule.oldId;
            delete updatedRule.EndDate;
            newCategory.Rules.push(updatedRule);
          }
        }
      }

      // Handle FinalAction
      if (this.checkFinalActionUpdated(category)) {
        newCategory.FinalAction = this.updatedFields[category].FinalAction;
        isCategoryUpdated = true;
        updateInfo.updated++;
      } else if (category === 'StorageRule' || category === 'AppraisalRule') {
        newCategory.FinalAction = this.updatedFields[category].FinalAction;
      }
      if (this.checkClassificationLevelUpdated(category) || this.checkClassificationOwnerUpdated(category)){
        newCategory.ClassificationLevel = this.updatedFields[category].ClassificationLevel;
        newCategory.ClassificationOwner = this.updatedFields[category].ClassificationOwner;
        isCategoryUpdated = true;
        updateInfo.updated++;
      }

      if (isCategoryUpdated) {
        const setAction = {};
        setAction[category] = newCategory;
        updateInfo.rules.push(setAction);
        updateInfo.categories.push(category);
      }
    }

    return updateInfo;
  }

  hasFinalActionEmptyRule(category) {
    if (category === 'StorageRule' || category === 'AppraisalRule') {
      if (this.management[category] && this.management[category].Rules) {
        for (const rule of this.management[category].Rules) {
          if (!rule.Rule && rule.FinalAction) {
            return true;
          }
        }
      }
    }
    return false;
  }

  // FIXME Unused method ?
  haveFinalActionWithEmptyRules(category): boolean {
    const mgtCategory = this.management[category];
    return mgtCategory && (!mgtCategory.Rules || mgtCategory.Rules.length === 0) && !!mgtCategory.FinalAction;
  }

  saveUpdate() {
    const info = this.getUpdatedRules();
    const rules = info.rules;
    if (rules.length > 0) {
      this.messageToDisplay = '';
      const rulesCategoriesFr =
        info.categories.map(
          title => {
            const filter = this.rulesCategories.filter(x => x.rule === title);
            return filter[0].label
          });
      if (info.categories.length === 1) {
        this.messageToDisplay = `Vous vous apprêtez à modifier la catégorie ${rulesCategoriesFr} pour :<br />`
      } else {
        this.messageToDisplay = `Vous vous apprêtez à modifier les catégories ${rulesCategoriesFr.join(', ')} pour :<br />`
      }
      if (info.deleted < 2) {
        this.messageToDisplay = this.messageToDisplay + `- Supprimer ${info.deleted} règle,<br />`;
      } else {
        this.messageToDisplay = this.messageToDisplay + `- Supprimer ${info.deleted} règles,<br />`;
      }
      if (info.updated < 2) {
        this.messageToDisplay = this.messageToDisplay + `- Modifier ${info.updated} règle,<br />`
      } else {
        this.messageToDisplay = this.messageToDisplay + `- Modifier ${info.updated} règles,<br />`
      }
      if (info.added < 2) {
        this.messageToDisplay = this.messageToDisplay + `- Ajouter ${info.added} règle`
      } else {
        this.messageToDisplay = this.messageToDisplay + `- Ajouter ${info.added} règles`
      }
      this.confirmationService.confirm({
        message: this.messageToDisplay,
        accept: () => {
          this.saveRunning = true;
          const request = [];
          request.push({'UpdatedRules': rules});
          this.archiveUnitService.updateMetadata(this.id, request).subscribe(
            () => {
              this.archiveUnitService.getDetails(this.id)
                .subscribe((data) => {
                  const response = data.$results[0];
                  this.inheritedRules = response.inheritedRule;
                  this.management = response['#management'];
                  this.id = response['#id'];
                  this.saveOriginal = JSON.stringify(this.management);
                  this.switchUpdateMode();
                  this.saveRunning = false;
                  this.displayOK = true;
                }, () => {
                  this.saveRunning = false;
                });
            },
            (error) => {
              this.saveRunning = false;
              this.buildErrorMessage(error.error.description);
              this.displayKO = true;
            }
          );
        }
      });
    } else {
      this.switchUpdateMode();
    }

  }

  buildErrorMessage(error) {
    switch (error) {
      case 'ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_EXIST':
      case 'ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_EXIST':
        this.errorMessage = 'La règle ajoutée n\'existe pas dans le référentiel.';
        break;
      case 'ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_CATEGORY':
      case 'ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_CATEGORY':
        this.errorMessage = 'La règle de gestion ajoutée n\'est pas de la bonne catégorie.';
        break;
      case 'ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_START_DATE':
      case 'ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_START_DATE':
        this.errorMessage = 'La date de départ de la règle de gestion est supérieure ou égale à 9000.';
        break;
      default:
        this.errorMessage = 'Echec lors de la mise à jour des règles.';
        break;
    }
  }

  removeRule(category, index, rule) {
    if (rule.newRule) {
      this.updatedFields[category].Rules.splice(index, 1);
    } else if (rule.inherited) {
      const preventedIndex = this.updatedFields[category].Inheritance.PreventRulesId.indexOf(rule.Rule);
      if (preventedIndex === -1) {
        this.updatedFields[category].Inheritance.PreventRulesId.push(rule.Rule);
      } else {
        this.updatedFields[category].Inheritance.PreventRulesId.splice(preventedIndex, 1);
      }
    } else {
      rule.oldRule = !rule.oldRule;
    }
  }


  initUpdatedRules() {
    for (const category of this.rulesCategories) {
      if (this.management[category.rule]) {
        const rules = [];
        const ruleIds = [];
        for (const rule of this.management[category.rule].Rules) {
          const updatedRule = JSON.parse(JSON.stringify(rule));
          if (updatedRule.StartDate) {
            updatedRule.StartDate = new Date(updatedRule.StartDate);
          } else {
            updatedRule.StartDate = '';
          }
          updatedRule.oldId = updatedRule.Rule;

          rules.push(updatedRule);
          ruleIds.push(updatedRule.Rule);
        }

        if (this.inheritedRules[category.rule]) {
          for (const ruleId in this.inheritedRules[category.rule]) {
            if (!this.inheritedRules[category.rule].hasOwnProperty(ruleId)) { continue; }
            if (ruleIds.indexOf(ruleId) !== -1) {
              continue;
            }
            for (const id in this.inheritedRules[category.rule][ruleId]) {
              if (!this.inheritedRules[category.rule][ruleId].hasOwnProperty(id)) { continue; }
              const inheritedRule = this.inheritedRules[category.rule][ruleId][id];

              const rule: any = {
                Rule: ruleId,
                StartDate: inheritedRule.StartDate ? new Date(inheritedRule.StartDate) : '',
                EndDate: inheritedRule.EndDate,
                inherited: true,
              };
              if (category.rule === 'StorageRule' || category.rule === 'AppraisalRule') {
                rule.FinalAction = inheritedRule.FinalAction;
              } else if (category.rule === 'ClassificationRule') {
                rule.ClassificationLevel = inheritedRule.ClassificationLevel;
                rule.ClassificationOwner = inheritedRule.ClassificationOwner;
              }
              rules.push(rule);
              break;
            }
          }
        }

        const inheritance = this.management[category.rule].Inheritance;
        const finalAction = this.management[category.rule].FinalAction;
        const classificationLevel = this.management[category.rule].ClassificationLevel;
        const classificationOwner = this.management[category.rule].ClassificationOwner;
          
        this.updatedFields[category.rule] = {
          Rules: rules
        };

        if (inheritance) {
          this.updatedFields[category.rule].Inheritance = JSON.parse(JSON.stringify(inheritance))
        } else {
          this.updatedFields[category.rule].Inheritance = {PreventRulesId: []};
        }

        if (finalAction) {
          this.updatedFields[category.rule].FinalAction = finalAction;
        }
        if (classificationLevel){
          this.updatedFields[category.rule].ClassificationLevel = classificationLevel;  
        }
        if (classificationOwner){
          this.updatedFields[category.rule].ClassificationOwner = classificationOwner;  
        }

      } else {

        const rules = [];
        if (this.inheritedRules[category.rule]) {
          for (const ruleId in this.inheritedRules[category.rule]) {
            if (this.inheritedRules[category.rule].hasOwnProperty(ruleId)) {
              // add rule
              for (const id in this.inheritedRules[category.rule][ruleId]) {
                if (this.inheritedRules[category.rule][ruleId].hasOwnProperty(id)) {
                  const inheritedRule = this.inheritedRules[category.rule][ruleId][id];

                  const rule: any = {
                    Rule: ruleId,
                    StartDate: inheritedRule.StartDate ? new Date(inheritedRule.StartDate) : '',
                    EndDate: inheritedRule.EndDate,
                    inherited: true
                  };
                  if (category.rule === 'StorageRule' || category.rule === 'AppraisalRule') {
                    rule.FinalAction = inheritedRule.FinalAction;
                  }
                  if (category.rule === 'ClassificationRule') {
                    rule.ClassificationLevel = inheritedRule.ClassificationLevel;
                    rule.ClassificationOwner = inheritedRule.ClassificationOwner;
                  }
                  rules.push(rule);
                  break;
                }
              }
            }
          }
        }

        this.updatedFields[category.rule] = {
          Rules: rules,
          Inheritance: {
            PreventRulesId: []
          }
        }
      }
    }
  }

  addRule(category) {
    this.updatedFields[category].Rules.push({
      Rule: '',
      StartDate: '',
      newRule: true
    });
  }

}
